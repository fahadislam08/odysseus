"""Idempotent runtime patch for local-only Journal RAG.

Applied on Odysseus container startup so the custom behavior survives a
container recreate without bind-mounting/forking whole upstream source files.
"""

from pathlib import Path
import re
import sqlite3


CHAT_PROCESSOR = Path("/app/src/chat_processor.py")
CHAT_HELPERS = Path("/app/routes/chat_helpers.py")
DB_PATH = Path("/app/data/app.db")

MARKER = "# ODYSSEUS_LOCAL_JOURNAL_RAG_PATCH"


def patch_chat_processor():
    text = CHAT_PROCESSOR.read_text(encoding="utf-8")

    # Make the chosen threshold persistent/configurable.
    if "import os\n" not in text[:300]:
        text = text.replace("import logging\n", "import logging\nimport os\n", 1)

    text = re.sub(
        r"RAG_SIMILARITY_THRESHOLD\s*=\s*[0-9.]+",
        'RAG_SIMILARITY_THRESHOLD = float(os.getenv("RAG_SIMILARITY_THRESHOLD", "0.35"))',
        text,
        count=1,
    )

    if MARKER not in text:
        needle = (
            "                    results = rag_manager.search("
            "message, k=5, owner=owner)\n"
        )

        if needle not in text:
            raise RuntimeError(
                "Could not find normal RAG search line in chat_processor.py"
            )

        insertion = needle + f"""                    {MARKER}
                    # Journal vectors are queried only when the selected model endpoint is local.
                    try:
                        from src.journal_rag_local import is_local_model_session, search_journal
                        if is_local_model_session(session):
                            journal_results = search_journal(message, k=5, owner=owner)
                            if journal_results:
                                results = list(results) + list(journal_results)
                                results.sort(
                                    key=lambda r: r.get("similarity", 0),
                                    reverse=True,
                                )
                                logger.info(
                                    "Journal RAG: merged %s local-only result(s)",
                                    len(journal_results),
                                )
                    except Exception as journal_exc:
                        logger.warning(
                            "Journal RAG retrieval failed: %s",
                            journal_exc,
                        )
"""

        text = text.replace(needle, insertion, 1)

    CHAT_PROCESSOR.write_text(text, encoding="utf-8")


def patch_chat_helpers():
    text = CHAT_HELPERS.read_text(encoding="utf-8")

    # Keep RAG on for ordinary chat/agent turns. Preserve the intentional
    # privacy boundary for incognito and research-spinoff sessions.
    old = (
        "if incognito or not allow_tool_preprocessing "
        "or is_research_spinoff or casual_low_signal:"
    )
    new = "if incognito or is_research_spinoff:"

    if old in text:
        text = text.replace(old, new, 1)

    CHAT_HELPERS.write_text(text, encoding="utf-8")


def force_chatgpt_tools():
    """Force native tool support on for ChatGPT/Codex endpoints."""

    if not DB_PATH.exists():
        print(
            f"[journal-patch] database not found at {DB_PATH}; "
            "skipping supports_tools patch",
            flush=True,
        )
        return

    conn = sqlite3.connect(DB_PATH)

    try:
        # Check that the expected table/column exist before touching anything.
        columns = {
            row[1]
            for row in conn.execute("PRAGMA table_info(model_endpoints)").fetchall()
        }

        if not columns:
            print(
                "[journal-patch] model_endpoints table not found; "
                "skipping supports_tools patch",
                flush=True,
            )
            return

        if "supports_tools" not in columns:
            print(
                "[journal-patch] model_endpoints.supports_tools not found; "
                "skipping supports_tools patch",
                flush=True,
            )
            return

        # Determine which URL column exists in this Odysseus version.
        if "base_url" in columns:
            url_column = "base_url"
        elif "endpoint_url" in columns:
            url_column = "endpoint_url"
        elif "url" in columns:
            url_column = "url"
        else:
            print(
                "[journal-patch] no recognized endpoint URL column found; "
                "skipping supports_tools patch",
                flush=True,
            )
            return

        cursor = conn.execute(
            f"""
            UPDATE model_endpoints
            SET supports_tools = 1
            WHERE {url_column} LIKE ?
            """,
            ("https://chatgpt.com/backend-api/codex%",),
        )

        conn.commit()

        print(
            "[journal-patch] forced supports_tools=true for "
            f"{cursor.rowcount} ChatGPT/Codex endpoint(s)",
            flush=True,
        )

    finally:
        conn.close()


def main():
    patch_chat_processor()
    patch_chat_helpers()
    force_chatgpt_tools()

    print(
        "[journal-patch] local-only Journal RAG patch active",
        flush=True,
    )


if __name__ == "__main__":
    main()