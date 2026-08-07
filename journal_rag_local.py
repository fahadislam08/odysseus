"""Local-only journal retrieval helper for Odysseus.

This module never reads raw journal files. It only queries the dedicated
FastEmbed-backed Chroma collection created by vault_watch.py.
"""

import ipaddress
import logging
import os
from urllib.parse import urlparse

from src.chroma_client import get_chroma_client
from src.embeddings import FastEmbedClient

logger = logging.getLogger(__name__)

COLLECTION = os.getenv("JOURNAL_COLLECTION", "journal_rag_fastembed")
VECTOR_WEIGHT = 0.7
KEYWORD_WEIGHT = 0.3
_embedder = None


def _get_embedder():
    global _embedder
    if _embedder is None:
        _embedder = FastEmbedClient()
    return _embedder


def is_local_model_session(session) -> bool:
    """Allow Journal RAG only for explicitly/local-network model endpoints."""
    endpoint = str(getattr(session, "endpoint_url", "") or "").strip()
    if not endpoint:
        return False

    try:
        host = (urlparse(endpoint).hostname or "").lower()
    except Exception:
        return False
    if not host:
        return False

    explicit = {
        h.strip().lower()
        for h in os.getenv(
            "JOURNAL_LOCAL_HOSTS",
            "localhost,127.0.0.1,host.docker.internal,::1",
        ).split(",")
        if h.strip()
    }
    if host in explicit:
        return True

    # Private/loopback IP endpoints are local by definition for this policy.
    try:
        ip = ipaddress.ip_address(host)
        return bool(ip.is_private or ip.is_loopback or ip.is_link_local)
    except ValueError:
        pass

    # Docker service names generally have no public DNS suffix.
    if "." not in host:
        return True

    return False


def search_journal(query: str, k: int = 5, owner: str | None = None):
    if not query or not isinstance(query, str):
        return []

    client = get_chroma_client()
    try:
        collection = client.get_collection(COLLECTION)
    except Exception:
        return []

    count = int(collection.count())
    if count <= 0:
        return []

    embedder = _get_embedder()
    vector = embedder.encode([query], normalize_embeddings=True)[0]
    if hasattr(vector, "tolist"):
        vector = vector.tolist()

    kwargs = {
        "query_embeddings": [vector],
        "n_results": min(max(k * 3, k), count),
        "include": ["documents", "metadatas", "distances"],
    }
    if owner:
        kwargs["where"] = {"owner": owner}

    results = collection.query(**kwargs)
    if not results.get("ids") or not results["ids"][0]:
        return []

    query_words = set(query.lower().split())
    candidates = []
    for idx, doc_id in enumerate(results["ids"][0]):
        doc = results["documents"][0][idx]
        meta = results["metadatas"][0][idx]
        distance = results["distances"][0][idx]

        vector_sim = 1.0 - distance
        doc_words = set(doc.lower().split())
        overlap = len(query_words & doc_words)
        keyword_score = overlap / len(query_words) if query_words else 0.0
        hybrid = (VECTOR_WEIGHT * vector_sim) + (KEYWORD_WEIGHT * keyword_score)

        candidates.append(
            {
                "id": doc_id,
                "document": doc,
                "metadata": meta,
                "distance": round(distance, 4),
                "similarity": round(hybrid, 4),
                "vector_similarity": round(vector_sim, 4),
                "keyword_score": round(keyword_score, 4),
                "embedding_lane": "journal_fastembed",
            }
        )

    candidates.sort(key=lambda r: r["similarity"], reverse=True)
    return candidates[:k]
