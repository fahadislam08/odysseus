# Changelog

## [2.0.0] - 2026-07-18

### Added
- Full native Android app — complete rewrite in Kotlin + Jetpack Compose
- **Chat** — multi-session AI chat, web search toggle, session management (create/rename/delete/archive)
- **Notes** — create/edit notes, todos with checkbox items, reminders, pin, archive, color labels
- **Tasks** — scheduled AI tasks (once/daily/weekly/monthly/cron), manual run, pause/resume, run history
- **Calendar** — event list, create/edit/delete, recurring events (rrule), CalDAV sync
- **Email** — inbox, multi-folder, read emails, compose, reply, AI-generated reply, flag, archive, delete, search
- **Documents** — full document editor (markdown/html/plain), library search, version-aware save
- **Gallery** — image grid, upload from device, albums, AI auto-tag, favorites, delete
- **Research** — start deep web research, live progress polling, full report view with sources list
- **Memory** — browse memories by category (fact/contact/task/preference/etc.), pin, delete; Skills tab
- **Settings** — server URL configuration, connection test, dark/light mode, change password, logout
- **FirstRunScreen** — guided server setup on first launch with connection test and EC2 checklist
- Cookie-based auth — uses `odysseus_session` cookie, identical to web browser
- Persistent preferences via DataStore (server URL, login state, dark mode)
- Material 3 dark/light theme matching Odysseus web palette
- Zero hardcoded IPs — fully portable, GitHub-safe

## [1.0.0] - 2026-07-18 (initial zip)

- Basic chat skeleton (non-functional, wrong API contract)
- Removed and replaced entirely in v2.0.0
