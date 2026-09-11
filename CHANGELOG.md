# Changelog

## 0.2.0 — 2026-09-11
- Major cover-screen usability pass: larger +/- controls, exact weight/reps entry, compact all-set previous performance and persisted exercise position.
- Global weight increment now defaults to 0.5 kg and is configurable in Settings; database support added for per-exercise increments.
- Added cover quick actions for skip set/exercise, add/remove sets, add/replace exercise, reorder exercise and finish workout.
- Workout overview exercises are tappable to jump directly to them; bottom safe area increased so Finish early clears system navigation.
- Added reps-only exercise support.
- Added robust local autosave/restoration, database migration v2 and database-backed undo of the last accounted set.
- Empty workouts are discarded. History distinguishes Completed vs Ended early and records skipped sets.
- Added subtle PR detection for highest weight / improved reps at the highest weight, plus a workout-completion summary.
- Expanded the built-in exercise library and added PUSH, PULL, LEGS, UPPER BODY and FULL BODY starter templates.
- Added exercise search for Add/Replace and a cover exercise-help view foundation.
- Fixed GitHub Actions artifact/version naming for this release.

## 0.1.1 — 2026-09-11
- Fixed unreadable dark-on-dark workout text by explicitly applying high-contrast colours.
- Added a dedicated portrait workout layout for the Galaxy Z Flip6 inner display.
- Kept the two-column workout layout for genuinely wide windows only.
- Added safe drawing insets so content clears Android status and navigation bars.
- Settings now displays the build version directly from BuildConfig.

## 0.1.0 — 2026-09-08
- Initial native Android release.
- Dedicated compact current-set UI and swipe overview.
- Adaptive unfolded workout/home UI.
- Offline SQLite persistence, resumable active workout, history and previous-set prefill.
- Sample CHEST + ARMS template and bundled exercise library.
- Haptics, undo, keep-awake setting and GitHub Actions APK build.
