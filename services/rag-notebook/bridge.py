"""Internal CRM adapter using RAG-Notebook's real hybrid retriever.

CRM supplies authorized source snapshots; this service never accepts a browser identity.
No customer text, credentials or model payloads are logged.
"""
import asyncio
import hashlib
import hmac
import json
import math
import os
import sqlite3
import threading
from collections import defaultdict
from pathlib import Path

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pydantic import BaseModel, Field

from app.rag.retrievers.hybrid_retriever import HybridRetriever

DATA = Path(os.getenv('RAG_DATA_DIRECTORY', '/data/rag'))
DATA.mkdir(parents=True, exist_ok=True)
SECRET = os.getenv('RAG_INTERNAL_TOKEN', '')
MODEL = os.getenv('RAG_EMBEDDING_MODEL', 'text-embedding-v4')
ENDPOINT = os.getenv('RAG_EMBEDDING_ENDPOINT', 'https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings')
KEY = os.getenv('RAG_EMBEDDING_API_KEY', '')
app = FastAPI(title='RAG-Notebook CRM internal adapter', docs_url=None, redoc_url=None)
locks = defaultdict(asyncio.Lock)
splitter = RecursiveCharacterTextSplitter(chunk_size=900, chunk_overlap=120,
    separators=['\n\n', '\n', '。', '！', '？', '；', ' ', ''])


def authenticated(x_rag_token: str = Header(default='')):
    if not SECRET or not hmac.compare_digest(x_rag_token, SECRET):
        raise HTTPException(401, 'Internal authentication required')


class CachedCloudEmbeddings(Embeddings):
    def __init__(self):
        self.lock = threading.RLock()
        with sqlite3.connect(DATA / 'embeddings.sqlite') as db:
            db.execute('CREATE TABLE IF NOT EXISTS embedding_cache (id TEXT PRIMARY KEY, value TEXT NOT NULL)')

    def embed_documents(self, texts):
        if not KEY:
            raise RuntimeError('Embedding service not configured')
        keys = [hashlib.sha256((MODEL + ':1024:' + text).encode()).hexdigest() for text in texts]
        result = [None] * len(texts)
        with self.lock, sqlite3.connect(DATA / 'embeddings.sqlite') as db:
            missing = []
            for i, key in enumerate(keys):
                row = db.execute('SELECT value FROM embedding_cache WHERE id=?', (key,)).fetchone()
                if row:
                    result[i] = json.loads(row[0])
                else:
                    missing.append(i)
            for offset in range(0, len(missing), 10):
                batch = missing[offset:offset + 10]
                with httpx.Client(timeout=60, follow_redirects=False) as client:
                    response = client.post(ENDPOINT, headers={'Authorization': 'Bearer ' + KEY},
                        json={'model': MODEL, 'input': [texts[i] for i in batch], 'dimensions': 1024})
                if response.status_code != 200:
                    raise RuntimeError('Embedding service HTTP ' + str(response.status_code))
                data = sorted(response.json()['data'], key=lambda item: item['index'])
                if len(data) != len(batch):
                    raise RuntimeError('Incomplete embedding result')
                for index, item in zip(batch, data):
                    vector = item['embedding']
                    if len(vector) != 1024 or not all(isinstance(v, (int, float)) and math.isfinite(v) for v in vector):
                        raise RuntimeError('Invalid embedding result')
                    result[index] = vector
                    db.execute('INSERT OR REPLACE INTO embedding_cache VALUES (?,?)', (keys[index], json.dumps(vector)))
            db.commit()
        return result

    def embed_query(self, text):
        return self.embed_documents([text])[0]


embedding = CachedCloudEmbeddings()
store = Chroma(collection_name='crm_rag_notebook', persist_directory=str(DATA / 'chroma'),
    embedding_function=embedding, collection_metadata={'hnsw:space': 'cosine'})
retriever = HybridRetriever(store)


class Source(BaseModel):
    id: str = Field(min_length=1, max_length=120)
    version: str = Field(min_length=1, max_length=128)
    title: str = Field(max_length=200)
    content: str = Field(min_length=1, max_length=120000)


class Scope(BaseModel):
    id: str = Field(pattern=r'^(company|customer:[A-Za-z0-9_-]{1,40})$')
    documents: list[Source] = Field(max_length=500)


class Search(BaseModel):
    query: str = Field(min_length=1, max_length=1200)
    scopes: list[Scope] = Field(min_length=1, max_length=2)
    top_k: int = Field(default=5, ge=1, le=8)


def snapshot(scope):
    """Stage a new immutable generation before dropping obsolete snapshot chunks."""
    digest = hashlib.sha256(json.dumps([d.model_dump() for d in scope.documents],
        sort_keys=True, ensure_ascii=False).encode()).hexdigest()
    generation = scope.id + ':' + digest
    ready = DATA / (hashlib.sha256(generation.encode()).hexdigest() + '.ready')
    active = DATA / (hashlib.sha256(scope.id.encode()).hexdigest() + '.active')
    if active.exists() and active.read_text() == generation and ready.exists() and (not scope.documents or store.get(where={'user_id': generation}, limit=1)['ids']):
        return generation
    documents = []
    ids = []
    for source in scope.documents:
        chunks = splitter.split_text(source.content)
        for index, text in enumerate(chunks):
            key = hashlib.sha256((generation + ':' + source.id + ':' + str(index)).encode()).hexdigest()
            ids.append(key)
            documents.append(Document(page_content=text, metadata={'user_id': generation,
                'scope_id': scope.id, 'source_id': source.id, 'source_version': source.version,
                'title': source.title, 'chunk': index}))
    if len(documents) > 10000:
        raise ValueError('Scope has too many chunks')
    # Chroma upsert is idempotent, so interrupted snapshot writes can be retried.
    for offset in range(0, len(documents), 100):
        store.add_documents(documents[offset:offset + 100], ids=ids[offset:offset + 100])
    ready.touch()
    old = store.get(where={'scope_id': scope.id}, include=['metadatas'])
    obsolete = [key for key, meta in zip(old['ids'], old['metadatas']) if meta['user_id'] != generation]
    for offset in range(0, len(obsolete), 1000):
        store.delete(ids=obsolete[offset:offset + 1000])
    active.write_text(generation)
    return generation


@app.get('/health')
def health():
    return {'status': 'UP', 'embeddingConfigured': bool(KEY), 'engine': 'RAG-Notebook hybrid'}


@app.post('/v1/search', dependencies=[Depends(authenticated)])
async def search(request: Search):
    if sum(len(d.content) for scope in request.scopes for d in scope.documents) > 3_000_000:
        raise HTTPException(413, 'Knowledge scope is too large; narrow the customer selection')
    matches = []
    try:
        for scope in request.scopes:
            async with locks[scope.id]:
                generation = await asyncio.to_thread(snapshot, scope)
                if not scope.documents:
                    continue
                engine = await retriever.get_retriever(request.query, generation)
                docs = await engine.ainvoke(request.query)
                allowed = {(d.id, d.version) for d in scope.documents}
                # Return only source IDs/versions from the authenticated caller's snapshot.
                for rank, doc in enumerate(docs):
                    meta = doc.metadata
                    if (meta.get('source_id'), meta.get('source_version')) not in allowed:
                        continue
                    matches.append({'id': meta['source_id'], 'version': meta['source_version'],
                        'title': meta['title'], 'content': doc.page_content, 'chunk': meta['chunk'],
                        'score': 1.0 / (rank + 1), 'scope': scope.id})
    except Exception as error:
        # Never emit source text, HTTP request headers or upstream response bodies.
        raise HTTPException(503, 'Knowledge retrieval unavailable: ' + type(error).__name__) from None
    matches.sort(key=lambda item: item['score'], reverse=True)
    return {'engine': 'RAG-Notebook', 'matches': matches[:request.top_k]}
