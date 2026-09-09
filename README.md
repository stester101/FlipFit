# FlipFit v0.1.0

FlipFit is a personal, offline-first native Android gym logger designed around the Samsung Galaxy Z Flip6. Kotlin + Jetpack Compose provide a dedicated compact workout UI when the app window is cover-sized and a richer layout when unfolded. Data is stored locally in SQLite; no account, backend or network connection is required.

## Samsung FlexWindow limitation

The APK is a normal Android application. Samsung does not automatically allow every third-party app to launch as a normal full app on the Z Flip6 FlexWindow. On software versions where FlipFit is not listed under **Settings > Advanced features > Labs > Apps allowed on cover screen**, install Samsung Good Lock and use **MultiStar > I ♡ Galaxy Foldable > Launcher Widget** to add FlipFit. Once Samsung launches the app on the cover display, FlipFit detects the small window and renders its dedicated compact UI. This project does not depend on a fictional or private Samsung API.

A native Samsung cover-screen *widget* is a separate integration from running the full app. v0.1.0 intentionally uses the full-app route because the logging UI needs interactive state and navigation; a Samsung-specific widget can be evaluated later.

## Repository root

Upload the **contents** of this folder to the root of the GitHub repository. Do not upload the ZIP itself. At GitHub root you should immediately see `app`, `gradle`, `.github`, `gradlew`, `build.gradle.kts`, and `settings.gradle.kts`.

## Build and install the APK

1. Open the repository on GitHub.
2. Open **Actions**.
3. Select **Build FlipFit APK**.
4. Select **Run workflow** and run it from `main`.
5. Wait for the build job to complete.
6. Open the completed workflow run.
7. Under **Artifacts**, download **FlipFit-v0.1.0-APK**.
8. Extract the downloaded artifact ZIP. It contains `FlipFit-v0.1.0-debug.apk`.
9. Open or transfer the APK to the Galaxy Z Flip6.
10. Android may ask you to allow that app (for example My Files or Chrome) to **Install unknown apps**. Enable it for that source.
11. Install the APK.

Debug APKs are automatically signed with the Android debug key. Release signing is intentionally deferred.

## Making changes

- `app/src/main/java/com/simontester/flipfit/model/` — data models.
- `app/src/main/java/com/simontester/flipfit/data/` — SQLite database and settings.
- `app/src/main/java/com/simontester/flipfit/ui/` — Compose UI, including compact/full adaptive layouts.
- `app/src/main/res/` — Android resources and vector launcher placeholder.
- `.github/workflows/build-apk.yml` — cloud APK build.
- `app/build.gradle.kts` — Android SDK, version and dependencies.

Edit files in GitHub, commit to `main`, then either let the push build run automatically or manually run the workflow.

## Architecture

The app uses a single-activity Compose architecture with a small `AndroidViewModel`, a native `SQLiteOpenHelper` database, and SharedPreferences for simple settings. Workout history is normalized into exercise, template, session, session-exercise and set tables. An unfinished session is represented by a session row with no end time, so it survives app closure, locking, process death and fold/unfold recreation.

Compact mode is selected from the live Compose window constraints (`<= 480dp` wide and `<= 600dp` high). It is a dedicated interface, not the full UI scaled down. Compose re-evaluates this when the window size changes.

## v0.1.0 workflow

The bundled **CHEST + ARMS** template contains Dumbbell Bench Press, Incline Dumbbell Press, Dumbbell Curl, Hammer Curl and Dumbbell Tricep Extension, each with three sets. Starting a workout opens the first incomplete exercise. The app prefills the matching set from the most recent completed session when available. Log Set persists immediately. After the target sets are logged, the next incomplete exercise becomes current. Swipe horizontally on the cover UI for the workout overview. The last logged set can be undone.

## Current limitations

- Template/exercise creation and editing are schema-ready but not exposed in the v0.1.0 UI; the bundled template is immediately usable.
- History is read-only and summarized on the unfolded home screen; detailed historical drill-down is a later release.
- KG is implemented in the workout UI. LB conversion/settings are planned after the core logger is validated on-device.
- The configurable increment is stored but v0.1.0 compact controls intentionally use fixed 2.5 kg buttons while the interaction is validated.
- No advanced PR/statistics screen yet.
- No cloud sync or backup.
- No dedicated Samsung FlexWindow widget in v0.1.0.

## Build stack

- Android Gradle Plugin 8.7.3
- Gradle 8.9 wrapper
- Kotlin 2.0.21
- Jetpack Compose BOM 2024.12.01
- compileSdk / targetSdk 35
- minSdk 28
- Java 17 in GitHub Actions
