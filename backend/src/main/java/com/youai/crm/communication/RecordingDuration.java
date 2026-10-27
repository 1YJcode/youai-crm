package com.youai.crm.communication;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioSystem;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Derives duration from the uploaded audio, never from browser form fields. */
final class RecordingDuration {
    private RecordingDuration() {}

    static int seconds(byte[] audio, String mediaType) {
        if ("audio/wav".equals(mediaType)) {
            try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
                return rounded(stream.getFrameLength() / (double) stream.getFormat().getFrameRate());
            } catch (Exception error) {
                throw invalid();
            }
        }
        String format = switch (mediaType) {
            case "audio/mpeg" -> "mp3";
            case "audio/mp4" -> "mov";
            case "audio/flac" -> "flac";
            case "audio/ogg" -> "ogg";
            default -> throw invalid();
        };
        java.nio.file.Path file = null;
        Process process = null;
        try {
            file = Files.createTempFile("youai-recording-", "." + format);
            Files.write(file, audio);
            String executable = System.getenv().getOrDefault("FFPROBE_PATH", "ffprobe");
            process = new ProcessBuilder(executable, "-v", "error", "-protocol_whitelist", "file,pipe",
                    "-f", format, "-i", file.toString(), "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) throw invalid();
            if (process.exitValue() != 0) throw invalid();
            String output = new String(process.getInputStream().readNBytes(1024), StandardCharsets.UTF_8).trim();
            return rounded(Double.parseDouble(output));
        } catch (java.io.IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "录音时长解析服务暂不可用，请联系管理员");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt(); throw invalid();
        } catch (NumberFormatException error) {
            throw invalid();
        } finally {
            if (process != null) process.destroyForcibly();
            if (file != null) try { Files.deleteIfExists(file); } catch (java.io.IOException ignored) { }
        }
    }

    static int seconds(java.nio.file.Path file, String mediaType) {
        if("audio/wav".equals(mediaType)) {
            try(var stream=AudioSystem.getAudioInputStream(file.toFile())) {
                return rounded(stream.getFrameLength()/(double)stream.getFormat().getFrameRate());
            } catch(Exception e) { throw invalid(); }
        }
        String format=switch(mediaType) {
            case "audio/mpeg" -> "mp3"; case "audio/mp4" -> "mov";
            case "audio/flac" -> "flac"; case "audio/ogg" -> "ogg"; default -> throw invalid();
        };
        Process process=null;
        try {
            process=new ProcessBuilder(System.getenv().getOrDefault("FFPROBE_PATH","ffprobe"),"-v","error",
                    "-protocol_whitelist","file,pipe","-f",format,"-i",file.toString(),"-show_entries","format=duration",
                    "-of","default=noprint_wrappers=1:nokey=1").redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(15,TimeUnit.SECONDS) || process.exitValue()!=0) throw invalid();
            return rounded(Double.parseDouble(new String(process.getInputStream().readNBytes(1024),StandardCharsets.UTF_8).trim()));
        } catch(java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"录音时长解析服务暂不可用，请联系管理员");
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw invalid(); }
        catch(NumberFormatException e) { throw invalid(); }
        finally { if(process!=null) process.destroyForcibly(); }
    }

    private static int rounded(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > Integer.MAX_VALUE) throw invalid();
        return (int) Math.max(1, Math.round(seconds));
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "无法读取录音时长，文件可能损坏，请重新导出");
    }
}
