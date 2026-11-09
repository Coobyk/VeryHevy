# Untangle refactor — decision log

## Fixes and the problems they solve

1. Container exposes repositories (workout/routine/exercise/settings/measurement); AppRoot `vhViewModel` factory helper.
   - VMs cast `app as VeryHevyApp` and each construct `WorkoutRepository(database)` (NEEDLESS: AppRoot:86-91, every VM)
   - HomeViewModel bypasses repos to measurementDao/exerciseDao (NEEDLESS)
   - `vm.exerciseRepository` public val leaks repo signature to UI (NEEDLESS)

2. Shared set-table/exercise-card primitives in Components.kt: `SetTableHeader`, `WeightRepsFields` + `rememberSetTextState`, `WarmupCell` (clickable), `ExerciseCardScaffold`, `AddExerciseButton`, `AddSetButton`, `InlineStat`, `LedgerRow`, `RPE_OPTIONS`, `nextRestSeconds`.
   - SetRow/RoutineSetRow ~80% dup + divergent commit semantics (unify: commit on focus-loss + check)
   - TableLabel dup, header/row weights hand-synced, RPE list dup, W-cell triplicated,
     rest-cycle expr dup, exercise-card scaffold dup, add-set/add-exercise buttons dup,
     equipment.replace('_',' ') ×3, "%.1f".format(rpe) ×5, digit filter ×4
   - Card-row pattern (LedgerCard+Row+trailing) ×3
   - Tracker onToggleWarmup dead param → W cell becomes the toggle (restores feature)
   - RoutineSetRow pencil-icon warmup hack → same W cell

3. Constants single-sourced: `WeightUnit.displayToKg/displayFromKg` on enum,
   `RestSeconds { DEFAULT=90, MAX=240, STEP=30, ADD=15 }`, `Plates { BAR_KG, PLATES_KG }`,
   `Format.date(epochDay)`, `Format.rpe()`, `Format.equipment()`, `Format.supersetLabel()`.
   - 90 in 7 places, +15s ×2, plate inventory in composable, kg/lb branch ×3,
     epochDay*86400000 ×2, superset lettering ×3

4. CSV import: shared `resolveExercise()` (match-or-custom + custom-id gen),
   `insertSets()`, hoist `exerciseDao().all()` out of loops, skip unparseable dates
   (count + report), wire legacy notes, MATCH_THRESHOLD const.
   - verbatim dup exercise-resolution blocks ×2, "Imported workout" ×2,
     parseDate?:now silent corruption, per-loop all() O(n×library), dead iNotes

5. BackupManager: `withTransaction` + DAO deleteAll (incl in-progress workouts),
   shared set/exercise JSON (de)serializers, shared readText(uri) w/ CsvImporter.
   - serialization blocks dup ×2 each side, delete skips in-progress (latent bug),
     row-by-row delete, no transaction

6. Repositories: shared private `PlanOps` (delete+reindex, move, template addSet,
   deleteSet+reindex, one-empty-set), `touch()` called on ALL routine mutations,
   drop dead methods, fix addSet return, remove `.let{}`, single `now()`.
   - reorder/move/reindex/template-copy algos verbatim dup ×2 (worst hidden link)

7. Analytics: `weeklyStreak` iterates week-keys directly (no 52 cap, no volume coupling);
   workoutCount counts sessions with ≥1 non-warmup set; delete dead totals/WorkoutTotals;
   PR record pairs fields from ONE set (store best set, derive fields).
   - streak coupled to volume bucketing + hardcoded 52, warmup-count inconsistency,
     bestWeightKg/bestReps from different sets

8. Calculators: delete dead `roundToIncrement`, move `formatWeight`→`Format.weight`,
   drop unused decimals param, add `PLATES_KG`/`BAR_KG` consts.
   - dead fn kept alive by own test, domain→UI formatting link

9. Format: rm dead `time()/timeFormat`, `duration(start,end)` non-null + `durationSince(start)`
   (kill `?: now` sentinel), add `rpe/equipment/supersetLabel/decimalFilter`.

10. Dead code purge: DAO methods w/o callers (deleteCustom, RoutineDao.count/observeById/
    deleteSetsFor, ExerciseDao.observeAll), SetWithWorkout.workoutName,
    repo pass-throughs w/o callers (observeInProgress, observeById, inProgress, updateNotes,
    RoutineDao.updateExerciseNotes/setSuperset/updateSet, ExerciseRepository observeAll/byId/all→keep all for name map),
    ExerciseLibraryCard, NumStat.align, unused imports (VhDivider ×3, BorderStroke,
    HomeViewModel flow imports + OptIn, LocalContext), weeklyVolumeOf,
    CsvRow.notes path→wired, `com.veryhevy.` FQNs in same package.

11. ViewModels: HistoryVM exposes `workoutDays` StateFlow (zone logic off UI);
    HomeVM emits `recordRows` (PR+name joined — kills UI merge + side fetch);
    RoutinesVM `current` via flatMapLatest on routine-id flow (kills 6 manual refreshes);
    TrackerVM previousSets uses empty-list not sentinel; RestAlarm extracted for vibrate/tone;
    VMs expose `searchExercises` wrapper.
    - screen LocalDate math (NEEDLESS), names side-fetch (NEEDLESS), refresh-by-convention
      (HIDDEN), sentinel pair (SPECIAL), platform code in VM (NEEDLESS)

12. Screens: adopt shared primitives (2-3 above); unit-keyed field remember
    (`remember(set.id, unit)`); HomeScreen chart `.take(12)` newest-first then ascending
    (fixes backwards window bug); EmptyState in the 3 inline spots; SettingsSection helper;
    SettingsViewModel → own file.
    - stale unit fields (HIDDEN), takeLast on DESC bug (HIDDEN), shell dup ×4 (HIDDEN)

13. AppDatabase: fallbackToDestructiveMigration() — v1, no migrations exist.
    - upgrade would crash otherwise (SPECIAL/safety)

14. Tests: delete roundToIncrement test+fn, tautological similarity assert,
    stale/wrong comments (":24", ":31"), tighten ambiguous parseDate assert.
    - tests that can't fail / mirror implementation / misleading comments

15. Keep (deliberate, not problems): entities' compose @Immutable (stability contract);
    routine supersetId/notes/createdAt fields (schema; wiring UI is feature work);
    secondaryMuscles/instructions (schema); backup `id`/`version`/`exportedAt`
    (forward-compat); oneRepMax reps==1 guard (domain rule); compileSdk 37/target 36;
    debug-signed release fallback; two custom-id schemes (deterministic hash for imports,
    timestamp for user-created — different needs); hashCode custom id; `synonyms` map shape;
    CsvRow vs cell() (different shapes, kept); RoutineExerciseWithSets FQN (cosmetic);
    LIMIT 50 search (fine); seed fire-and-forget (fine); `time`→keep clock().
