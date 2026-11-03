import os
import uuid
import unittest
from unittest.mock import patch
from fastapi import FastAPI
from fastapi.testclient import TestClient
from test_bridge import bridge, vector
from incremental import install


class IncrementalTest(unittest.TestCase):
    def setUp(self):
        self.scope='customer:'+uuid.uuid4().hex
        self.client=TestClient(bridge.app)
        self.headers={'X-Rag-Token':'internal-test-token'}
        self.embedding=patch.object(bridge.embedding,'embed_documents',side_effect=lambda texts:[vector(t) for t in texts])
        self.query=patch.object(bridge.embedding,'embed_query',side_effect=vector)
        self.embedding.start();self.query.start()

    def tearDown(self):
        self.embedding.stop();self.query.stop()

    def change(self,id='segment:1',version='1',text='客户提出预算顾虑',expected='',delete=False):
        return self.client.post('/v2/sync',headers=self.headers,json={'scope':self.scope,'id':id,'expected':expected,
            'document':None if delete else {'id':id,'version':version,'title':'案例','content':text}})

    def search(self,version='1',scope=None):
        return self.client.post('/v2/search',headers=self.headers,json={'query':'预算顾虑','scopes':[
            {'id':scope or self.scope,'documents':[{'id':'segment:1','version':version}]}]})

    def test_delta_update_does_not_rewrite_other_sources(self):
        first=self.change().json()['token']
        second=self.change(id='segment:2',text='另一份资料').json()['token']
        with patch.object(bridge.incremental_store,'add_documents',wraps=bridge.incremental_store.add_documents) as write:
            updated=self.change(version='2',text='新的客户预算',expected=first)
            self.assertEqual(updated.status_code,200)
            self.assertEqual(write.call_count,1)
            self.assertTrue(all(d.metadata['source_id']=='segment:1' for d in write.call_args.args[0]))
        self.assertTrue(bridge.incremental_store.get(where={'user_id':second})['ids'])
        self.assertFalse(bridge.incremental_store.get(where={'user_id':first})['ids'])
        self.assertTrue(self.search('2').json()['matches'])
        self.assertEqual(self.search().json()['matches'],[])

    def test_delete_cleans_without_any_search(self):
        token=self.change().json()['token']
        self.assertEqual(self.change(expected=token,delete=True).status_code,200)
        self.assertFalse(bridge.incremental_store.get(where={'user_id':token})['ids'])
        self.assertEqual(self.search().json()['matches'],[])

    def test_missing_version_is_pending_and_other_scope_never_matches(self):
        self.change()
        self.assertEqual(self.search('2').json()['pending'],1)
        self.assertEqual(self.search(scope='company').json()['matches'],[])
        payload={'query':'预算','scopes':[{'id':self.scope,'documents':[{'id':'segment:1','version':'1','content':'不允许全文'}]}]}
        self.assertEqual(self.client.post('/v2/search',headers=self.headers,json=payload).status_code,422)
        self.assertEqual(self.client.get('/v2/catalog').status_code,401)
        self.assertEqual(self.client.post('/v2/cleanup').status_code,401)

    def test_compare_and_swap_rejects_stale_worker(self):
        token=self.change().json()['token']
        self.assertEqual(self.change(version='2').status_code,409)
        self.assertEqual(self.change(expected=token,version='2').status_code,200)
        self.assertEqual(self.change(expected=token,delete=True).status_code,409)
        self.assertTrue(self.search('2').json()['matches'])

    def test_catalog_survives_service_recreation(self):
        token=self.change().json()['token']
        app=FastAPI()
        install(app,bridge.authenticated,bridge.DATA,bridge.incremental_store,bridge.splitter,bridge.incremental_retriever)
        with TestClient(app) as client:
            rows=client.get('/v2/catalog',headers=self.headers).json()['sources']
            self.assertTrue(any(row['scope']==self.scope and row['token']==token for row in rows))

    def test_failed_embedding_keeps_previous_version_then_retries(self):
        token=self.change().json()['token']
        with patch.object(bridge.incremental_store,'add_documents',side_effect=RuntimeError('offline')):
            with self.assertRaises(RuntimeError):
                self.change(expected=token,version='2',text='新的内容')
        self.assertTrue(self.search().json()['matches'])
        self.assertEqual(self.change(expected=token,version='2',text='新的内容').status_code,200)

    def test_thirty_queries_keep_deleted_version_until_cleanup(self):
        import asyncio
        from incremental import Query, Change
        token=self.change().json()['token']
        routes={r.path:r.endpoint for r in bridge.app.routes if hasattr(r,'endpoint')}
        async def scenario():
            entered=asyncio.Event();release=asyncio.Event();count=0
            class Engine:
                async def ainvoke(self, query):
                    nonlocal count
                    count+=1
                    if count==30: entered.set()
                    await release.wait()
                    return []
            async def engine(*args): return Engine()
            request=Query(query='预算',scopes=[{'id':self.scope,'documents':[{'id':'segment:1','version':'1'}]}])
            with patch.object(bridge.incremental_retriever,'get_retriever',side_effect=engine):
                tasks=[asyncio.create_task(routes['/v2/search'](request)) for _ in range(30)]
                try:
                    await asyncio.wait_for(entered.wait(),10)
                    await routes['/v2/sync'](Change(scope=self.scope,id='segment:1',expected=token))
                    self.assertTrue(bridge.incremental_store.get(where={'user_id':token})['ids'])
                finally:
                    release.set()
                    await asyncio.gather(*tasks)
            await routes['/v2/cleanup']()
            self.assertFalse(bridge.incremental_store.get(where={'user_id':token})['ids'])
        asyncio.run(scenario())

    def test_legacy_cleanup_removes_old_plaintext(self):
        scope=bridge.Scope(id=self.scope,documents=[bridge.Source(id='segment:1',version='1',title='old',content='旧存储敏感正文')])
        bridge.snapshot(scope)
        with patch.dict(os.environ,{'RAG_LEGACY_ENABLED':'false'}):
            self.assertEqual(self.client.post('/v2/cleanup',headers=self.headers,json={}).status_code,200)
        self.assertFalse(bridge.store.get(where={'scope_id':self.scope})['ids'])

if __name__=='__main__':
    unittest.main()
