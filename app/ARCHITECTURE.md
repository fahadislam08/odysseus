# Odysseus Architecture & Documentation

## Overview
Odysseus is a modern Android application built using Kotlin and Jetpack Compose. It serves as a unified AI assistant platform, integrating a robust chat interface similar to Gemini, with extended custom features tailored for the "Odysseus" persona.

The application follows a clean architectural pattern, separating concerns into distinct layers: UI (Screens/Components), ViewModels (State Management), Repository (Data Handling), and Data Sources (Local/Remote).

## Directory Structure

`app/src/main/java/com/odysseus/wrapper/`

### 1. `ui/`
Handles all the visual elements, user interactions, and screens.
- **`screens/chat/`**: Contains `ChatScreen.kt` and `ChatViewModel.kt`. The ChatScreen is responsible for rendering the chat messages, user input, and interacting with the ViewModel. The ViewModel manages the state (current session, message list) and handles logic.
- **`screens/settings/`**: Contains `SettingsScreen.kt`. It manages user preferences, profile details, theme switching, and access to archived chats.
- **`screens/login/`**: Contains `LoginScreen.kt`. It provides the user authentication interface.
- **`theme/`**: Contains styling, colors, and typography settings for Material 3.

### 2. `data/`
Responsible for data fetching, persistence, and external API communication.
- **`local/`**: Contains Room Database definitions (`AppDatabase.kt`), Data Access Objects (`ChatDao.kt`, `ChatSessionDao.kt`), and Entities (`ChatMessageEntity.kt`, `ChatSessionEntity.kt`). It provides offline support and stores chat histories.
- **`remote/`**: Contains API integration files.
  - `OdysseusApiService.kt` & `ApiClient.kt`: Define and provide Retrofit instances for any custom Odysseus backend.
  - `GeminiApiClient.kt`: A fallback integration with Google's Gemini API in case the primary Odysseus server is unreachable or disabled.
- **`repository/`**: Contains `ChatRepository.kt`. The Repository acts as a single source of truth, abstracting whether data comes from the local database or the remote API. It orchestrates the flow of saving messages locally and fetching responses from the server.

### 3. `MainActivity.kt`
The primary entry point of the app. It hosts the Navigation graph (using `NavHost`), defining the routes to `login`, `chat`, and `settings`. It also contains the Navigation Drawer (side menu) for easy session switching and global actions.

## How it Works & Server Connection

### Message Flow
1. **User Input:** The user types a message in `ChatScreen` and clicks send.
2. **ViewModel Action:** The `ChatViewModel` receives this intent and calls `ChatRepository.sendMessage(sessionId, content)`.
3. **Local Storage (Immediate):** The Repository immediately stores the user's message in the Room Database via `ChatDao.insertMessage()`. This makes it instantly visible on the screen.
4. **API Request (Primary Server):** The Repository makes a network call to the primary Odysseus Server using `ApiClient.apiService.sendMessage()`.
5. **Fallback to Gemini (Failover):** If the primary server fails, times out, or returns an error, the Repository catches the exception and falls back to `fallbackToGemini()`, utilizing `GeminiApiClient` with the provided `GEMINI_API_KEY`.
6. **AI Response Storage:** The AI's response (from either server) is then saved to the local database, which automatically updates the UI since the UI observes the database flow (`collectAsStateWithLifecycle`).

### How to Configure

1. **API Keys:**
   API keys should be managed via the AI Studio Secrets panel. The `GEMINI_API_KEY` is loaded through `BuildConfig`.
   - Ensure you define your `GEMINI_API_KEY` in the environment secrets.

2. **Custom Backend URL:**
   If you have a primary server for Odysseus, update the base URL in `app/src/main/java/com/odysseus/wrapper/data/remote/ApiClient.kt`.
   - Replace the default placeholder URL with your actual API endpoint.

3. **Database Migration:**
   If you add new fields to `ChatMessageEntity` or `ChatSessionEntity`, increment the `version` number in `AppDatabase.kt` and provide a migration strategy or use `fallbackToDestructiveMigration()` during development.
