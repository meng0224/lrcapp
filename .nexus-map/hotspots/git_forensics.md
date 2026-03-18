> generated_by: nexus-mapper v2
> verified_at: 2026-03-18
> provenance: Derived from `.nexus-map/raw/git_stats.json` over the last 90 days of local git history.

# Git Forensics

## Hotspots

- `app/src/main/java/com/example/lrcapp/MainActivity.kt` — 15 changes — high
- `README.md` — 11 changes — medium
- `app/src/main/java/com/example/lrcapp/util/StorageHelper.kt` — 10 changes — medium
- `app/src/main/res/layout/activity_main.xml` — 10 changes — medium
- `app/src/main/java/com/example/lrcapp/util/SettingsManager.kt` — 6 changes — medium

Interpretation:
- 產品演進重心在主流程與保存/授權 UX，而不是演算法本身。
- 文檔也同步頻繁更新，代表行為規格仍在收斂。
- `SubtitleConverter.kt` 只出現 4 次，說明格式轉換規則近期相對穩定。

## Strong Coupling Pairs

- `MainActivity.kt` <-> `activity_main.xml` — score 1.00, 10 co-changes
- `MainActivity.kt` <-> `StorageHelper.kt` — score 0.80, 8 co-changes
- `StorageHelper.kt` <-> `StorageHelperTest.kt` — score 1.00, 6 co-changes
- `MainActivity.kt` <-> `SettingsManager.kt` — score 1.00, 6 co-changes
- `SettingsManager.kt` <-> `StorageHelper.kt` — score 0.83, 5 co-changes
- `MainActivity.kt` <-> `SubtitleFileAdapter.kt` — score 1.00, 5 co-changes

## Risk Reading

- Critical boundary pressure sits between UI orchestration and storage/authorization behavior, not between UI and subtitle parsing.
- `StorageHelper.kt` has healthy co-change with its test file, which lowers change risk compared with similarly hot but less directly isolated UI logic.
- The high `MainActivity` pairings imply that feature work often spans layout, persisted settings, and save-path behavior together. Treat edits there as workflow changes, not isolated refactors.

## Practical Review Priority

- review `MainActivity.kt` first for any user-visible workflow change
- then inspect `StorageHelper.kt` and `SettingsManager.kt` for permission and persistence regressions
- finally check the impacted tests, especially `StorageHelperTest.kt` and `MainActivityInstrumentationTest.kt`
