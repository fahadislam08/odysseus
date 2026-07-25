<p align="center">
  <img src="docs/odysseus-wordmark.png" alt="Odysseus" width="238">
</p>

<p align="center">
  A self-hosted AI workspace for chat, agents, research, documents, email, notes, calendar, and local model workflows — with a native Android app.
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> ·
  <a href="docs/setup.md">Setup Guide</a> ·
  <a href="#android-app">Android App</a> ·
  <a href="CONTRIBUTING.md">Contributing</a> ·
  <a href="ROADMAP.md">Roadmap</a>
</p>

<p align="center">
  <a href="https://github.com/vyber07/odysseus/releases/latest">
    <img src="https://img.shields.io/github/v/release/vyber07/odysseus?label=latest&color=e06c75" alt="Latest Release">
  </a>
  <a href="https://github.com/vyber07/odysseus/releases/latest">
    <img src="https://img.shields.io/badge/Android%20APK-download-50fa7b?logo=android" alt="Android APK">
  </a>
</p>

<p align="center">
  <img src="docs/odysseus-browser.jpg" alt="Odysseus interface">
</p>

---

## Quick Start

> `dev` is the default branch — newest changes first.

```bash
git clone https://github.com/vyber07/odysseus.git
cd odysseus
cp .env.example .env
docker compose up -d --build
```

Open `http://YOUR_SERVER_IP` — the container binds to `0.0.0.0:80` by default so it's reachable from any device on any network. The first admin password is printed in `docker compose logs odysseus`.

> **Defaults:** `APP_BIND=0.0.0.0` · `APP_PORT=80` · change in `.env` if needed.

Native installs, GPU notes, Windows/macOS, HTTPS and config details live in the [setup guide](docs/setup.md).

---

## Features

### Core
- **Chat + Agents** — local/API models, tools, MCP, files, shell, skills, and persistent memory.
- **Cookbook** — hardware-aware model recommendations, downloads, and serving (vLLM, llama.cpp, Ollama, SGLang).
- **Deep Research** — multi-step web research with source reading and report generation.
- **Compare** — blind side-by-side model testing and synthesis.

### Productivity
- **Documents** — writing-first editor with AI edits, Markdown/HTML/CSV, syntax highlighting.
- **Email** — IMAP/SMTP inbox, triage, tags, summaries, reminders, AI reply drafts.
- **Notes** — sticky notes, todos with checkboxes, reminders, pin/archive, colour labels.
- **Tasks** — scheduled AI tasks (daily/weekly/cron), manual run, run history.
- **Calendar** — create/edit events, recurring rules, CalDAV sync.
- **Memory** — per-user long-term memory extraction, skills library.

### Extras
- **Gallery** — image grid, upload, albums, AI auto-tagging, image editor.
- **Themes** — dark/light + full colour customisation, density, font options, background effects.
- **Uploads** — files attached to chat, document imports (PDF, DOCX, XLSX).
- **Web search** — SearXNG, Tavily, Google, DDG integrations.
- **Presets** — save and share model/prompt/tool configurations.
- **2FA** — TOTP two-factor auth per account.
- **Multi-user** — per-user data isolation, admin panel, API tokens.

### 📱 Mobile App (new in v2.1.0)
- Native Android app — full feature parity with the web UI.
- **Settings → Mobile App** panel: generate connect tokens, scan QR, manage sessions.
- Bearer-token auth — the app works without cookies, through any network.
- See [Android App](#android-app) section below.

---

## Android App

Download the latest APK from the [Releases page](https://github.com/vyber07/odysseus/releases/latest) and install on any Android 8.0+ device.

### Connect in 3 steps

1. **Install** `app-debug.apk` on your Android device.
2. **Open the app** → enter your Odysseus server URL (e.g. `http://YOUR_EC2_IP/`).
3. **Login** with your username and password — or use a Mobile Token for passwordless access.

### Mobile Token (recommended)

Tokens let the app authenticate without relying on session cookies, making connections more stable across networks.

1. In the **Odysseus webapp** go to **Settings → Mobile App**.
2. Give the token a name (e.g. *My Phone*) and click **Generate Token**.
3. Copy the `ody_…` token **or scan the QR code** in the app's first-run screen.
4. The app stores the token and injects it as `Authorization: Bearer ody_…` on every request.

### Session management (webapp)

**Settings → Mobile App → Active Mobile Sessions** lists every token connected from a mobile device. Tap **Revoke** next to any token you don't recognise.

### What the app can do

| Screen | Capabilities |
|---|---|
| **Chat** | Multi-session AI chat, web search toggle, session list, rename/delete |
| **Email** | Inbox, folder picker, read/reply/AI-reply, flag/archive/delete, compose |
| **Notes** | Create/edit notes & todos with checkboxes, reminders, pin, colour, archive |
| **Tasks** | Schedule AI tasks, manual run, pause/resume, run history |
| **Calendar** | View events by date, create/edit/delete, recurring, CalDAV sync |
| **Documents** | Document library, full editor, create/save/archive |
| **Gallery** | Image grid, upload from phone, albums, AI auto-tag, favourites |
| **Research** | Start deep research, live progress bar, full report + sources |
| **Memory** | Browse/add memories by category, skills management |
| **Settings** | Server URL, connection test, dark mode, change password, logout |

### Requirements

- Android 8.0+ (API 26+)
- Odysseus server publicly accessible (EC2, VPS, or home server with port forwarding)
- Port 80 open to the internet (or whatever port you set in `.env`)

---

## API Tokens

Odysseus supports scoped API tokens for programmatic access and mobile connections.

### Mobile token profile

The `mobile_app` profile bundles all scopes needed by the Android app:

```
mobile  chat  todos:read  todos:write  documents:read  documents:write
email:read  email:draft  calendar:read  calendar:write  memory:read  memory:write
```

### Endpoints (no admin required — user-level)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/tokens/mobile` | List your active mobile tokens |
| `POST` | `/api/tokens/mobile` | Create a new mobile token (form field: `name`) |
| `DELETE` | `/api/tokens/mobile/{id}` | Revoke a mobile token |

### Usage

```http
Authorization: Bearer ody_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## Configuration

Key `.env` variables:

```bash
APP_BIND=0.0.0.0   # Bind address — 0.0.0.0 = all interfaces (default)
APP_PORT=80        # Host port — the container maps this to internal 7000
AUTH_ENABLED=true  # Always keep true on public servers
```

Full list in [`.env.example`](.env.example).

---

## Security

Odysseus is a self-hosted workspace with powerful local tools. Keep `AUTH_ENABLED=true`, keep private data out of Git, and do not expose raw model/service ports publicly. See the [setup guide](docs/setup.md#security-notes) and [THREAT_MODEL.md](THREAT_MODEL.md).

---

## Contributing

Help is welcome. Best entry points: fresh-install testing, provider setup bugs, mobile polish, docs, and focused refactors. See [CONTRIBUTING.md](CONTRIBUTING.md) and [ROADMAP.md](ROADMAP.md).

---

## License

AGPL-3.0-or-later — see [LICENSE](LICENSE) and [ACKNOWLEDGMENTS.md](ACKNOWLEDGMENTS.md).
