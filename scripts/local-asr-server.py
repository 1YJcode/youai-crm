"""Private single-worker local transcription service. Run with project-local Python."""
import argparse
import io
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
from pathlib import Path

from faster_whisper import WhisperModel

parser = argparse.ArgumentParser()
parser.add_argument('--model', default='large-v3')
parser.add_argument('--device', choices=['cpu', 'cuda'], default='cpu')
parser.add_argument('--port', type=int, default=8766)
parser.add_argument('--host', default='127.0.0.1')
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent / '.local-asr'
model = WhisperModel(args.model, device=args.device,
                     compute_type='int8' if args.device == 'cpu' else 'float16',
                     download_root=str(root / 'models'), cpu_threads=4)
transcription_lock = Lock()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass  # Never log customer audio or transcript.

    def reply(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == '/':
            page = Path(__file__).with_name('local-asr-trial.html').read_bytes()
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(page)))
            self.end_headers()
            return self.wfile.write(page)
        self.reply(200 if self.path == '/health' else 404,
                   {'ready': True, 'model': args.model, 'device': args.device} if self.path == '/health' else {})

    def do_POST(self):
        origin = self.headers.get('Origin')
        if origin and origin not in (f'http://127.0.0.1:{args.port}', f'http://localhost:{args.port}'):
            return self.reply(403, {'error': 'origin_not_allowed'})
        if self.path != '/transcribe':
            return self.reply(404, {})
        if not transcription_lock.acquire(blocking=False):
            self.close_connection = True
            return self.reply(429, {'error': 'busy'})
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 < length <= 200 * 1024 * 1024:
                return self.reply(413, {'error': 'audio_size'})
            self.connection.settimeout(30)
            audio = self.rfile.read(length)
            if len(audio) != length:
                return self.reply(400, {'error': 'incomplete_audio'})
            segments, info = model.transcribe(io.BytesIO(audio), language='zh',
                                              vad_filter=True, beam_size=5,
                                              condition_on_previous_text=False)
            if info.duration > 7200:
                return self.reply(422, {'error': 'duration_limit'})
            rows = []
            for segment in segments:
                text = segment.text.strip()
                if text:
                    rows.append({'start': round(segment.start, 2), 'end': round(segment.end, 2), 'text': text})
            if not rows:
                return self.reply(422, {'error': 'no_speech'})
            text = '\n'.join(f"[{r['start']:.2f}s–{r['end']:.2f}s] {r['text']}" for r in rows)
            if len(text) > 200000:
                return self.reply(422, {'error': 'transcript_limit'})
            self.reply(200, {'text': text, 'segments': rows, 'model': args.model})
        except Exception:
            self.reply(500, {'error': 'transcription_failed'})
        finally:
            transcription_lock.release()


print(f'Local ASR ready: {args.host}:{args.port}, model={args.model}, device={args.device}', flush=True)
ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
