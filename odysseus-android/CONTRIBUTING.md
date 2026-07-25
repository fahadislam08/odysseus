# Contributing

## Getting Started

1. Fork the repo
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/Odysses-app.git`
3. Open in Android Studio (Hedgehog or newer)
4. Let Gradle sync

## Development Setup

You need a running Odysseus server. The easiest way:
- AWS EC2 with Docker: follow the [server README](https://github.com/pewdiepie-archdaemon/odysseus)
- Or run locally with `docker compose up -d` and use `http://10.0.2.2/` in the Android emulator

## Project Structure

```
app/src/main/java/com/odysseus/wrapper/
├── core/                     # Network client, preferences
├── data/remote/              # API models + Retrofit interfaces
├── data/repository/          # Repository layer
└── ui/
    ├── components/           # Shared UI components
    ├── screens/              # One folder per feature module
    └── theme/                # Material 3 colors + theme
```

## Adding a New Feature

1. Add request/response data classes to `data/remote/Models.kt`
2. Add the Retrofit interface method to `data/remote/ApiServices.kt`
3. Add a repository method to `data/repository/Repositories.kt`
4. Create `ui/screens/yourfeature/YourViewModel.kt`
5. Create `ui/screens/yourfeature/YourScreen.kt`
6. Register in `MainActivity.kt` — add a `Screen` object and a `composable()` in the `NavHost`

## Code Style

- Kotlin idiomatic code
- ViewModel holds all state as `StateFlow`
- Screens are stateless Composables that observe ViewModels
- All network calls go through the repository layer
- No hardcoded IPs or secrets anywhere in code

## Pull Request Checklist

- [ ] No hardcoded IPs, tokens, or credentials
- [ ] New screen registered in `MainActivity.kt`
- [ ] ViewModel follows existing pattern (StateFlow, viewModelScope, safeCall)
- [ ] Tested on a real device or emulator connected to Odysseus server

## Reporting Issues

Open an issue with:
- Android version
- Server version / setup
- Steps to reproduce
- Logcat output if applicable
