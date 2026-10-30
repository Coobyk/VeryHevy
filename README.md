# VeryHevy

Like Hevy but without the slop I don't need — a free and open source workout tracker for Android, built with Kotlin, Jetpack Compose and Material 3 Expressive (dynamic color).

## Features

- **Workout tracker** — add exercises from a 190-exercise library (or create your own), log weight × reps per set, RPE, warmup sets, per-exercise notes, and superset grouping. The previous session's best set is shown while you lift.
- **Rest timer** — auto-starts when you complete a set (configurable per exercise), with +15s / skip controls, vibration and sound.
- **Routines** — plan workouts ahead of time and start a workout straight from a routine; duplicate routines.
- **History** — full workout log with a month calendar and detailed workout view.
- **Analytics (all free)** — weekly volume chart, volume by muscle group, estimated 1RM personal records, workout count and weekly streak.
- **Body weight tracking** — log measurements and see a trend chart.
- **Plate calculator** — what to load per side for any target weight.
- **Units** — kg/lb, switchable anytime (weights are always stored in kg).
- **Data ownership** — manual JSON backup export/import (SAF), plus a **Hevy CSV importer** so your existing history comes along.

## Building

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (debug-signed)
./gradlew test              # unit tests
```

Requirements: JDK 21, Android SDK with API 37 platform. The app targets Android 16 (API 36) and runs on API 26+.

## CI

Every push to `main` is built by GitHub Actions, which runs the unit tests and publishes both APKs as artifacts and as a GitHub release (`build-N`).

## Notes

- The exercise library is original content written for VeryHevy — it is not derived from Hevy's proprietary database.
- Portrait-only, phone-only by design. No backend, no accounts: your data stays on your device (and in your manual backups).
