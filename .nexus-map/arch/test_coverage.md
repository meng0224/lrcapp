> generated_by: nexus-mapper v2
> verified_at: 2026-03-18
> provenance: Derived from static inspection of test sources only; no Gradle or device-backed test execution was performed in this run.

# Static Test Coverage

## JVM Unit Tests

- `converter/SubtitleConverterTest.kt`
  - covers supported subtitle formats, ASS text-column parsing, text cleanup, and unsupported extension fallback.
- `util/StorageHelperTest.kt`
  - covers nested directory resolution, document reuse/creation, saved-file verification, and output result counting.
- `util/SettingsManagerTest.kt`
  - covers settings hydration and stable encoded preference keys for source/import-root directories.
- `util/FileSelectionPolicyTest.kt`
  - covers append-vs-replace semantics and URI-based deduplication.
- `util/FileValidatorTest.kt`, `util/FileNameHelperTest.kt`, `util/OutputSettingsPolicyTest.kt`, `util/FileListUiPolicyTest.kt`
  - cover pure validation, naming and small UI/state policy helpers.
- `model/FileStatusTest.kt`
  - covers `isEligibleForConversion()` state semantics.

## Instrumentation Tests

- `androidTest/MainActivityInstrumentationTest.kt`
  - verifies cold-launch empty state, default storage summary, source-directory mode restoration, and recursive-import switch restoration.

## Coverage Assessment

- strongest coverage:
  - pure converter logic
  - StorageHelper helper behavior
  - settings/policy helpers
- moderate coverage:
  - launch-time UI state restoration
- evidence gaps:
  - no static evidence of instrumentation coverage for real file picking, SAF re-authorization, recursive directory scanning, or end-to-end save success/failure UX
  - no static evidence that `MainActivity.startConversion()` full coroutine flow is isolated by unit tests
  - no test execution happened here because local Gradle/JDK setup is repo-specific and may fail without configured `JAVA_HOME` / `GRADLE_USER_HOME`

## Change Guidance

- if editing `SubtitleConverter.kt`, prefer JVM unit tests first.
- if editing `StorageHelper.kt` or `SettingsManager.kt`, update the existing util test classes in the same change.
- if changing launch text, empty state, or storage-mode UI, update `MainActivityInstrumentationTest.kt`.
