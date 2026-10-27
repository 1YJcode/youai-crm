# RAG-Notebook CRM adapter

Source: https://github.com/1YJcode/RAG-Notebook
Pinned revision: 9fcc700d2bb84f9bd0321aa7387d9da5c3aea2b0

Vendored modules: backend/app/rag/retrievers/{hybrid_retriever,empty_retriever}.py.
The hybrid retriever is used directly for BM25 + Chroma reciprocal-rank retrieval. Local change: jieba tokenization for Chinese BM25. The small config shim replaces the personal notebook app's configuration import.

The CRM bridge adds authenticated internal API, per-customer/shared scope isolation, persistent cloud embeddings, atomic scope snapshots and reference metadata. It uses LangChain text splitters and Chroma as in the upstream project. It does not deploy upstream personal accounts, public upload routes, note editor, destructive database reset recovery, or document-content logging. DeepSeek generation and CRM authorization remain in the CRM backend. Heavy local reranker/HyDE modules are not enabled in this adapter.

Upstream repository currently contains no LICENSE file. No license grant is asserted by this adapter. Keep upstream attribution; confirm redistribution rights before redistributing its source.
