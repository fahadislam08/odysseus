import hashlib
import os
import time
from pathlib import Path

from src.chroma_client import get_chroma_client
from src.embeddings import FastEmbedClient
from src.index_walk import prune_index_dirs, is_indexable_file
from src.rag_singleton import get_rag_manager
from src.rag_vector import DEFAULT_FILE_EXTENSIONS

NORMAL_ROOTS = [
    "/app/data/personal_docs/Vault Mind",
    "/app/data/personal_docs/AI Mind",
]
JOURNAL_ROOT = os.getenv("JOURNAL_ROOT", "/app/journal")
JOURNAL_COLLECTION = os.getenv("JOURNAL_COLLECTION", "journal_rag_fastembed")
OWNER = os.getenv("ODYSSEUS_ADMIN_USER", "admin")
INTERVAL = int(os.getenv("VAULT_WATCH_INTERVAL", "2"))


class JournalStore:
    """Local-FastEmbed-only journal index in a dedicated Chroma collection."""

    def __init__(self, chunker):
        self.chunker = chunker
        self.embedder = FastEmbedClient()
        self.dimension = int(self.embedder.get_sentence_embedding_dimension())
        self.client = get_chroma_client()
        self.collection = self.client.get_or_create_collection(
            name=JOURNAL_COLLECTION,
            metadata={
                "hnsw:space": "cosine",
                "embedding_lane": "fastembed",
                "embedding_url": "local://fastembed",
                "embedding_model": self.embedder.model,
                "embedding_dimension": self.dimension,
                "sensitivity": "journal-local-only",
            },
        )
        print(
            f"[vault-watch] journal collection ready: {JOURNAL_COLLECTION} "
            f"(docs={self.collection.count()}, model={self.embedder.model})",
            flush=True,
        )

    @staticmethod
    def _doc_id(text, source, chunk_id, owner):
        raw = f"{owner}\0{source}\0{chunk_id}\0{text}".encode("utf-8")
        return "journal_" + hashlib.sha256(raw).hexdigest()[:24]

    def _encode(self, texts):
        vectors = self.embedder.encode(texts, normalize_embeddings=True)
        return vectors.tolist() if hasattr(vectors, "tolist") else [list(v) for v in vectors]

    def delete_by_source(self, source):
        try:
            result = self.collection.get(
                where={"source": source},
                include=["metadatas"],
            )
            ids = result.get("ids") or []
            if ids:
                self.collection.delete(ids=ids)
            return len(ids)
        except Exception as exc:
            print(f"[vault-watch] journal delete failed {source}: {exc}", flush=True)
            return 0

    def remove_directory(self, directory):
        directory = os.path.abspath(directory)
        result = self.collection.get(include=["metadatas"])
        ids = []
        for doc_id, meta in zip(result.get("ids") or [], result.get("metadatas") or []):
            source = meta.get("source") if isinstance(meta, dict) else None
            if isinstance(source, str) and (
                source == directory or source.startswith(directory + os.sep)
            ):
                ids.append(doc_id)
        if ids:
            self.collection.delete(ids=ids)
        return len(ids)

    def index_file(self, path):
        self.delete_by_source(path)
        if not os.path.exists(path):
            return 0

        text = read_document(path)
        if not text or not text.strip():
            return 0

        ext = Path(path).suffix.lower()
        chunks = self.chunker(text)
        if not chunks:
            return 0

        ids = []
        metadatas = []
        for i, chunk in enumerate(chunks):
            ids.append(self._doc_id(chunk, path, i, OWNER))
            metadatas.append(
                {
                    "source": path,
                    "filename": os.path.basename(path),
                    "directory": os.path.dirname(path),
                    "type": ext,
                    "chunk_id": i,
                    "owner": OWNER,
                    "sensitivity": "journal",
                }
            )

        added = 0
        for start in range(0, len(chunks), 100):
            batch_chunks = chunks[start:start + 100]
            batch_ids = ids[start:start + 100]
            batch_meta = metadatas[start:start + 100]
            self.collection.add(
                ids=batch_ids,
                embeddings=self._encode(batch_chunks),
                documents=batch_chunks,
                metadatas=batch_meta,
            )
            added += len(batch_ids)
        return added

    def full_sync(self):
        if not os.path.isdir(JOURNAL_ROOT):
            print(f"[vault-watch] missing journal directory: {JOURNAL_ROOT}", flush=True)
            return
        removed = self.remove_directory(JOURNAL_ROOT)
        indexed = 0
        failed = 0
        for path in walk_files(JOURNAL_ROOT):
            try:
                indexed += self.index_file(path)
            except Exception as exc:
                failed += 1
                print(f"[vault-watch] journal index failed {path}: {exc}", flush=True)
        print(
            f"[vault-watch] journal sync complete: removed={removed} "
            f"indexed={indexed} failed_files={failed}",
            flush=True,
        )


def walk_files(base):
    if not os.path.isdir(base):
        return []
    paths = []
    for root, dirs, names in os.walk(base):
        prune_index_dirs(dirs)
        for name in names:
            if not is_indexable_file(name):
                continue
            path = os.path.join(root, name)
            if Path(path).suffix.lower() not in DEFAULT_FILE_EXTENSIONS:
                continue
            paths.append(path)
    return paths


def scan():
    files = {}
    for base in [*NORMAL_ROOTS, JOURNAL_ROOT]:
        for path in walk_files(base):
            try:
                stat = os.stat(path)
                files[path] = (stat.st_mtime_ns, stat.st_size)
            except FileNotFoundError:
                pass
    return files


def read_document(path):
    ext = Path(path).suffix.lower()
    if ext == ".pdf":
        from src.personal_docs import extract_pdf_text
        return extract_pdf_text(path)
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def normal_index_file(rag, path):
    rag.delete_by_source(path)
    if not os.path.exists(path):
        return
    text = read_document(path)
    if not text or not text.strip():
        return

    ext = Path(path).suffix.lower()
    docs = []
    for i, chunk in enumerate(rag._split_into_chunks(text)):
        docs.append(
            (
                chunk,
                {
                    "source": path,
                    "filename": os.path.basename(path),
                    "directory": os.path.dirname(path),
                    "type": ext,
                    "chunk_id": i,
                    "owner": OWNER,
                },
            )
        )
    if docs:
        result = rag.add_documents_batch(docs)
        print(
            f"[vault-watch] indexed {path} "
            f"(added={result.get('added_count', 0)}, "
            f"failed={result.get('failed_count', 0)})",
            flush=True,
        )


def full_sync_normal(rag):
    for root in NORMAL_ROOTS:
        if not os.path.isdir(root):
            print(f"[vault-watch] missing directory: {root}", flush=True)
            continue
        print(f"[vault-watch] syncing {root}", flush=True)
        remove_result = rag.remove_directory(root)
        index_result = rag.index_personal_documents(root, owner=OWNER)
        print(
            f"[vault-watch] sync complete {root}: "
            f"removed={remove_result.get('removed_count', 0)} "
            f"indexed={index_result.get('indexed_count', 0)} "
            f"failed={index_result.get('failed_count', 0)}",
            flush=True,
        )


def is_journal(path):
    journal = os.path.abspath(JOURNAL_ROOT)
    candidate = os.path.abspath(path)
    return candidate == journal or candidate.startswith(journal + os.sep)


def main():
    rag = None
    while rag is None:
        rag = get_rag_manager()
        if rag is None:
            print("[vault-watch] waiting for normal RAG/Chroma...", flush=True)
            time.sleep(5)

    journal = JournalStore(rag._split_into_chunks)

    full_sync_normal(rag)
    journal.full_sync()
    previous = scan()

    print(
        f"[vault-watch] watching {len(previous)} files every {INTERVAL}s "
        f"(journal collection={JOURNAL_COLLECTION})",
        flush=True,
    )

    while True:
        time.sleep(INTERVAL)
        current = scan()
        deleted = previous.keys() - current.keys()
        changed = {
            path
            for path, signature in current.items()
            if path not in previous or previous[path] != signature
        }

        failed_paths = set()

        for path in deleted:
            try:
                if is_journal(path):
                    removed = journal.delete_by_source(path)
                    print(f"[vault-watch] journal deleted {path} ({removed} chunks)", flush=True)
                else:
                    removed = rag.delete_by_source(path)
                    print(f"[vault-watch] deleted {path} ({removed} chunks)", flush=True)
            except Exception as exc:
                failed_paths.add(path)
                print(f"[vault-watch] delete failed {path}: {exc}", flush=True)

        for path in changed:
            try:
                if is_journal(path):
                    added = journal.index_file(path)
                    print(f"[vault-watch] journal indexed {path} (added={added})", flush=True)
                else:
                    normal_index_file(rag, path)
            except Exception as exc:
                failed_paths.add(path)
                print(f"[vault-watch] failed {path}: {exc}", flush=True)

        # Keep failed items dirty so they retry on the next poll.
        next_previous = dict(current)
        for path in failed_paths:
            if path in previous:
                next_previous[path] = previous[path]
            else:
                next_previous.pop(path, None)
        previous = next_previous


if __name__ == "__main__":
    main()
