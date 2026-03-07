# Phase 2 Validation Report

本文件只負責記錄 Phase 2 的驗證執行結果。當前整體狀態請看 [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)。

## Current Status

- Status: `NOT STARTED`
- Owner:
- Last Updated: 2026-03-08

## 1. Scope

- Android 11+
  - 冷啟動不跳 all-files-access
  - 可直接選檔
  - 可選擇 SAF 目錄並保存
- Android 7~10
  - 第一次選檔正確要求 `READ_EXTERNAL_STORAGE`
  - 授權後可正常選檔與保存
- 共通回歸
  - 無效檔案維持 `INVALID`
  - 同名輸出不先刪舊檔

## 2. Environment

- Repo: `D:\Git\lrcapp`
- Date:
- Tester:
- Device / Emulator:
- Android Version:
- Build Variant:

## 3. Automated Validation Status

### Unit tests

- Command:
  - `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
  - `set GRADLE_USER_HOME=D:\Git\lrcapp\.gradle-user`
  - `gradlew.bat testDebugUnitTest`
- Current status: `BLOCKED`
- Blocking reason:
  - Gradle wrapper attempts to download `gradle-9.0-milestone-1-bin.zip`
  - Current environment blocks outbound network access

### Instrumentation tests

- Candidate command:
  - `gradlew.bat connectedDebugAndroidTest`
- Current status: `NOT RUN`
- Minimum test candidate:
  - `MainActivityInstrumentationTest.coldLaunchShowsMainUiWithoutPermissionGate`

## 4. Manual Validation Checklist

### Android 11+

- [ ] Cold launch does not open all-files-access settings
- [ ] Tap `選擇文件` opens system document picker directly
- [ ] Select a valid subtitle file and item enters `待處理`
- [ ] Tap `選擇目錄` and select SAF directory
- [ ] Output directory text shows `SAF 授權目錄`
- [ ] Tap `開始轉換` and file saves successfully
- [ ] Re-run same output name without deleting previous file first

### Android 7~10

- [ ] Cold launch does not request storage permission automatically
- [ ] First tap on `選擇文件` requests `READ_EXTERNAL_STORAGE`
- [ ] Grant permission and document picker opens normally
- [ ] Valid subtitle converts and saves successfully

### Shared regression

- [ ] Unsupported extension becomes `無效`
- [ ] File larger than `10MB` becomes `無效`
- [ ] ASS/SSA text with commas remains intact
- [ ] SRT/VTT multiline text merges correctly

## 5. Results

### Passed

- 

### Failed

- 

### Blocked

- Unit test execution blocked by Gradle wrapper network download
- Instrumentation execution pending device/emulator availability

## 6. Follow-up

- If Gradle distribution is pre-provisioned, rerun unit tests and record result here
- Run `connectedDebugAndroidTest` on available emulator/device
- Update `Status` and checklist after each validation round
