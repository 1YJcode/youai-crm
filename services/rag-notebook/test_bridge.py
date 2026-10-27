import os
import hashlib
import math
import unittest
from unittest.mock import patch, MagicMock

os.environ.setdefault('RAG_INTERNAL_TOKEN', 'internal-test-token')
os.environ.setdefault('RAG_EMBEDDING_API_KEY', 'test-key')
os.environ.setdefault('RAG_DATA_DIRECTORY', '/tmp/rag-bridge-test')
import bridge
from fastapi.testclient import TestClient


def vector(text):
    values = [0.0] * 1024
    for token in text:
        values[int(hashlib.sha256(token.encode()).hexdigest()[:8], 16) % 1024] += 1
    size = math.sqrt(sum(v*v for v in values)) or 1
    return [v/size for v in values]


class BridgeTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(bridge.app)
        self.headers = {'X-Rag-Token': 'internal-test-token'}
        self.documents = patch.object(bridge.embedding, 'embed_documents', side_effect=lambda texts: [vector(t) for t in texts])
        self.query = patch.object(bridge.embedding, 'embed_query', side_effect=vector)
        self.documents.start(); self.query.start()

    def tearDown(self):
        self.documents.stop(); self.query.stop()

    def search(self, scope, text, source_id='article:1', version='1'):
        return self.client.post('/v1/search', headers=self.headers, json={'query':'处理客户顾虑', 'scopes':[{'id':scope,'documents':[{'id':source_id,'version':version,'title':'案例','content':text}]}]})

    def test_authentication_and_real_upstream_retrieval_scope(self):
        valid={'query':'x','scopes':[{'id':'company','documents':[]}]}
        self.assertEqual(self.client.post('/v1/search', json=valid).status_code,401)
        self.assertEqual(self.client.post('/v1/search', headers={'X-Rag-Token':'wrong'}, json=valid).status_code,401)
        personal=self.search('customer:123','PRIVATE 客户提出预算顾虑','segment:8')
        self.assertEqual(personal.status_code,200)
        company=self.search('company','先确认客户顾虑，再约定回访。')
        self.assertEqual(company.status_code,200)
        self.assertTrue(company.json()['matches'])
        self.assertNotIn('PRIVATE',str(company.json()))
        self.assertTrue(all(m['id']=='article:1' for m in company.json()['matches']))

    def test_updated_snapshot_and_reverted_scope_rebuild(self):
        self.assertEqual(self.search('customer:456','原案例处理顾虑','segment:4','1').status_code,200)
        changed=self.search('customer:456','新案例核实需求','segment:4','2')
        self.assertTrue(all(m['version']=='2' for m in changed.json()['matches']))
        restored=self.search('customer:456','原案例处理顾虑','segment:4','1')
        self.assertTrue(restored.json()['matches'])
        self.assertTrue(all(m['version']=='1' for m in restored.json()['matches']))

    def test_embedding_cache_uses_cloud_only_once_for_identical_input(self):
        self.documents.stop();self.query.stop()
        try:
            with patch('bridge.httpx.Client') as factory:
                response=MagicMock();response.status_code=200;response.json.return_value={'data':[{'index':0,'embedding':[1.0]+[0.0]*1023}]}
                factory.return_value.__enter__.return_value.post.return_value=response
                first=bridge.embedding.embed_query('UNIQUE-CACHE-TEST')
                second=bridge.embedding.embed_query('UNIQUE-CACHE-TEST')
                self.assertEqual(first,second);self.assertEqual(factory.return_value.__enter__.return_value.post.call_count,1)
        finally:
            self.documents.start();self.query.start()

    def test_empty_snapshot_clears_obsolete_chunks_and_can_be_restored(self):
        self.assertEqual(self.search('customer:789','待撤回的客户资料','segment:9').status_code,200)
        response=self.client.post('/v1/search',headers=self.headers,json={'query':'客户','scopes':[{'id':'customer:789','documents':[]}]})
        self.assertEqual(response.json()['matches'],[])
        self.assertEqual(bridge.store.get(where={'scope_id':'customer:789'})['ids'],[])
        restored=self.search('customer:789','待撤回的客户资料','segment:9')
        self.assertTrue(restored.json()['matches'])


if __name__=='__main__':
    unittest.main()
