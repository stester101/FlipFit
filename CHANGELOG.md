# Changelog

## 0.4.0 — 2026-09-12
- Full FlipFit visual redesign based on the approved restrained black/lime reference direction.
- Replaced the oversized top chip navigation with a persistent compact bottom navigation shell.
- Reworked Home into a focused dashboard: quick start, recent workout and a small template preview instead of the entire template library.
- Added a restrained liquid-glass visual system: translucent charcoal surfaces, fine outlines, subtle lime glass accents and tighter typography/spacing.
- Reworked unfolded workout presentation to match the new design while retaining all workout functionality.
- Refined Exercises, Templates, History and PR surfaces to use the same compact card language.
- Cover-screen workout remains deliberately minimal and high-contrast; functionality is preserved rather than hidden by decoration.
- Existing workout persistence, PR logic, editing, skipping, adding/replacing/reordering exercises, undo, history and management features remain intact.

## 0.3.0 — 2026-09-11
- Fixed the FlexWindow quick-actions menu by replacing the clipped dropdown with a safely sized, fully scrollable cover-screen dialog.
- Manual weight/reps entry now focuses immediately and selects the whole existing value so typing replaces it.
- Added live PR preview before logging: NEW PR appears beside the selected weight, while existing best weight/reps remain visible for context.
- Moved the post-log PR confirmation away from the set counter so SET x / y always stays visible.
- Marked the current movement graphics explicitly as placeholders pending the proper exercise-specific diagram artwork.
- Added a full unfolded Exercise Library manager: create, edit, favourite, duplicate, soft-delete, tracking type, default sets, diagram hint and per-exercise increment.
- Added a full unfolded Template builder: create, rename, duplicate, delete, add/remove exercises, reorder and change set counts.
- Added tappable workout History with detailed set breakdown, skipped sets, PR markers and total volume.
- Added a Personal Records screen showing highest weight, most reps at that weight and estimated 1RM.
- Database migrated to v3 with soft-archiving for exercises so workout history remains intact when an exercise is removed from the library.
- Updated GitHub Actions artifact/version naming to v0.3.0.

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
