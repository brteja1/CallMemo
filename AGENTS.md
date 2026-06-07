# Repository Guidelines

## Project Structure & Module Organization

- `app/src/main/java/com/example/androidcallnotes/` contains the app code.
- `data/` holds the Room entity, DAO, database, and repository.
- `receiver/` contains the phone-state `BroadcastReceiver`.
- `service/` contains the foreground overlay service and Compose overlay UI.
- `app/src/main/res/` stores strings and theme resources.
- Root files such as `settings.gradle.kts`, `build.gradle.kts`, and `gradle/wrapper/` define the Gradle build.

## Build, Test, and Development Commands

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew test` runs local JVM tests, if present.
- `./gradlew lint` runs Android lint checks.
- `./gradlew clean` removes build output and cached intermediates.

## Coding Style & Naming Conventions

- Use Kotlin, 4-space indentation, and idiomatic Android naming.
- Name classes by role: `CallReceiver`, `OverlayService`, `CallNotesDatabase`.
- Use PascalCase for types, camelCase for functions and properties, and lowercase package names.
- Keep Compose UI code focused and state-driven; avoid blocking calls on the main thread.
- Follow the existing Gradle Kotlin DSL formatting in `*.kts` files.

## Testing Guidelines

- Prefer unit tests for repository and state logic under `app/src/test/`.
- Use instrumentation tests for overlay and Android framework behavior under `app/src/androidTest/`.
- Name tests by behavior, for example `insertNote_savesRow()` or `callEnded_launchesOverlay()`.
- Keep database and coroutine work off the UI thread during tests as well.

## Commit & Pull Request Guidelines

- This workspace does not expose usable git history, so no commit convention could be verified directly.
- Use short, imperative commit messages, such as `Add Room database layer`.
- Pull requests should summarize the change, list validation steps, and include screenshots or screen recordings for UI work.

## Security & Configuration Tips

- Do not add network permissions; the app is intended to stay offline.
- Keep overlay and phone-state permissions explicit and minimal.
- Preserve the foreground-service flow for Android 12+ broadcast restrictions.
