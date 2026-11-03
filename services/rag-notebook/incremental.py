"""Incremental internal protocol. Catalog is authoritative only for index readiness.
Business authorization remains in CRM; search requires explicit source/version references.
Run one uvicorn worker: reader leases and staging guards are process-local.
"""
import asyncio
import hashlib
import json
import sqlite3
from collections import Counter

from fastapi import Depends, HTTPException
from pydantic import BaseModel, Field, ConfigDict
from langchain_core.documents import Document


class Reference(BaseModel):
    model_config = ConfigDict(extra='forbid')
    id: str = Field(min_length=1, max_length=120)
    version: str = Field(min_length=1, max_length=128)


class IndexedSource(Reference):
    title: str = Field(max_length=200)
    content: str = Field(min_length=1, max_length=120000)


class ScopeRefs(BaseModel):
    model_config = ConfigDict(extra='forbid')
    id: str = Field(pattern=r'^(company|customer:[A-Za-z0-9_-]{1,40})$')
    documents: list[Reference] = Field(max_length=500)


class Query(BaseModel):
    model_config = ConfigDict(extra='forbid')
    query: str = Field(min_length=1, max_length=1200)
    scopes: list[ScopeRefs] = Field(min_length=1, max_length=2)
    top_k: int = Field(default=6, ge=1, le=8)


class Change(BaseModel):
    scope: str = Field(pattern=r'^(company|customer:[A-Za-z0-9_-]{1,40})$')
    id: str = Field(min_length=1, max_length=120)
    expected: str = Field(default='', max_length=128)
    document: IndexedSource | None = None


def install(app, authenticate, data, store, splitter, retriever, forget=None, cleanup_legacy=None):
    catalog_path = data / 'catalog.sqlite'
    gate = asyncio.Lock()
    writer = asyncio.Lock()
    readers = Counter()
    staged = set()
    with sqlite3.connect(catalog_path) as db:
        db.execute('CREATE TABLE IF NOT EXISTS sources (scope TEXT, id TEXT, version TEXT, token TEXT, PRIMARY KEY(scope,id))')

    def inventory():
        with sqlite3.connect(catalog_path) as db:
            return [dict(zip(('scope', 'id', 'version', 'token'), row)) for row in db.execute('SELECT scope,id,version,token FROM sources')]

    def commit(change, token):
        with sqlite3.connect(catalog_path) as db:
            if change.document is None:
                db.execute('DELETE FROM sources WHERE scope=? AND id=?', (change.scope, change.id))
            else:
                db.execute('INSERT OR REPLACE INTO sources VALUES (?,?,?,?)',
                           (change.scope, change.id, change.document.version, token))

    async def remove_rows(rows, obsolete):
        for offset in range(0, len(obsolete), 500):
            await asyncio.to_thread(store.delete, ids=obsolete[offset:offset+500])
        if forget and obsolete:
            obsolete_ids = set(obsolete)
            await asyncio.to_thread(forget, [text for key, text in zip(rows['ids'], rows['documents']) if key in obsolete_ids])
        if obsolete:
            retriever._bm25_cache.clear()
        return len(obsolete)

    async def collect():
        # Writer prevents publication during collection. New readers can only pin
        # active tokens; existing readers were captured under gate.
        async with gate:
            active = {s['token'] for s in inventory()} | set(readers) | staged
        rows = await asyncio.to_thread(store.get, include=['metadatas'])
        obsolete = [key for key, meta in zip(rows['ids'], rows['metadatas']) if meta['user_id'] not in active]
        for offset in range(0, len(obsolete), 500):
            batch = await asyncio.to_thread(store.get, ids=obsolete[offset:offset+500], include=['documents'])
            await remove_rows(batch, batch['ids'])
        return len(obsolete)

    async def retire(token):
        if not token:
            return
        async with gate:
            if readers.get(token) or any(s['token'] == token for s in inventory()):
                return
        rows = await asyncio.to_thread(store.get, where={'user_id': token}, include=['documents'])
        await remove_rows(rows, rows['ids'])

    @app.get('/v2/catalog', dependencies=[Depends(authenticate)])
    async def catalog():
        async with gate:
            return {'sources': inventory()}

    @app.post('/v2/sync', dependencies=[Depends(authenticate)])
    async def sync(change: Change):
        if change.document and change.document.id != change.id:
            raise HTTPException(422, 'Source ID mismatch')
        async with writer:
            async with gate:
                current = next((s for s in inventory() if s['scope'] == change.scope and s['id'] == change.id), None)
                if (current['token'] if current else '') != change.expected:
                    raise HTTPException(409, 'Index changed; reconcile again')
            token = None
            if change.document:
                token = hashlib.sha256(json.dumps([change.scope, change.document.model_dump()], sort_keys=True, ensure_ascii=False).encode()).hexdigest()
                staged.add(token)
                chunks = splitter.split_text(change.document.content)
                documents = [Document(page_content=text, metadata={
                    'user_id': token, 'scope_id': change.scope, 'source_id': change.id,
                    'source_version': change.document.version, 'title': change.document.title, 'chunk': i
                }) for i, text in enumerate(chunks)]
                try:
                    for offset in range(0, len(documents), 100):
                        batch = documents[offset:offset+100]
                        await asyncio.to_thread(store.add_documents, batch,
                            ids=[token + ':' + str(i) for i in range(offset, offset+len(batch))])
                    async with gate:
                        await asyncio.to_thread(commit, change, token)
                finally:
                    staged.discard(token)
            else:
                async with gate:
                    await asyncio.to_thread(commit, change, token)
            await retire(current['token'] if current else None)
            return {'token': token, 'state': 'READY' if token else 'DELETED'}

    @app.post('/v2/cleanup', dependencies=[Depends(authenticate)])
    async def cleanup():
        async with writer:
            removed = await collect()
            if cleanup_legacy:
                await cleanup_legacy()
            return {'removed': removed}

    @app.post('/v2/search', dependencies=[Depends(authenticate)])
    async def search(request: Query):
        matches = []
        pending = 0
        for scope in request.scopes:
            async with gate:
                indexed = {(s['scope'], s['id'], s['version']): s['token'] for s in inventory()}
                tokens = sorted({indexed[(scope.id, d.id, d.version)] for d in scope.documents
                                 if (scope.id, d.id, d.version) in indexed})
                pending += sum((scope.id, d.id, d.version) not in indexed for d in scope.documents)
                readers.update(tokens)
            try:
                if not tokens:
                    continue
                key = 'v2:' + hashlib.sha256(json.dumps(tokens).encode()).hexdigest()
                source_filter = {'user_id': {'$in': tokens}}
                # Cache build is single-flight. Queries execute outside both locks.
                async with gate:
                    engine = await retriever.get_retriever(request.query, key, source_filter)
                docs = await engine.ainvoke(request.query)
                allowed = {(d.id, d.version) for d in scope.documents}
                for rank, doc in enumerate(docs):
                    meta = doc.metadata
                    if meta['scope_id'] != scope.id or (meta['source_id'], meta['source_version']) not in allowed:
                        continue
                    matches.append({'id': meta['source_id'], 'version': meta['source_version'],
                        'title': meta['title'], 'content': doc.page_content, 'chunk': meta['chunk'],
                        'score': 1.0/(rank+1), 'scope': scope.id})
            finally:
                async with gate:
                    for token in tokens:
                        readers[token] -= 1
                        if not readers[token]:
                            del readers[token]
        matches.sort(key=lambda m: m['score'], reverse=True)
        return {'matches': matches[:request.top_k], 'pending': pending}

    return cleanup
