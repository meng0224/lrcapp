# Phase 3 Test Hardening

本文件只負責記錄 Phase 3 的測試資產、執行方式、阻塞與後續 instrumentation 範圍。當前 phase 狀態請看 [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)。Phase 2 的手動驗證與裝置結果請看 [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md)。

## 1. Test Assets Added

### Unit tests already added

- `FileStatusTest`
  - `INVALID` 不可進入轉換
  - `PENDING` / `ERROR` 可進入轉換
- `FileValidatorTest`
  - 支援格式大小寫驗證
  - 不支援格式與無副檔名
  - `10MB` 邊界與 `10MB + 1`
- `FileNameHelperTest`
  - 停用 smart naming 時只替換副檔名
  - `music.mp3.ass -> music.lrc`
  - 一般 compound name 不被誤傷
- `SubtitleConverterTest`
  - ASS/SSA 逗號保留
  - ASS/SSA 樣式碼與 `\\N` 清理
  - SRT 基本轉換
  - VTT 多行合併
  - SMI 基本轉換
  - SUB 基本轉換
  - unsupported extension 回傳 `null`
  - 空的 supported content 回傳空字串
- `StorageHelperTest`
  - 既有 document 重用
  - 缺少 document 時建立新檔
  - 成功統計只計算非 `null` 結果

### Instrumentation tests already added

- `MainActivityInstrumentationTest`
  - 冷啟動可顯示主畫面主要 UI，不受全域權限流程阻塞

## 2. Execution Commands

### Unit tests

- `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `set GRADLE_USER_HOME=D:\Git\lrcapp\.gradle-user`
- `gradlew.bat testDebugUnitTest`

### Instrumentation tests

- `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `set GRADLE_USER_HOME=D:\Git\lrcapp\.gradle-user`
- `gradlew.bat connectedDebugAndroidTest`

## 3. Current Blockers

### Unit test execution

- Status: `BLOCKED`
- Blocking reason:
  - Gradle wrapper still attempts to download `gradle-9.0-milestone-1-bin.zip`
  - Current environment blocks outbound network access

### Instrumentation execution

- Status: `BLOCKED`
- Blocking reason:
  - No available device / emulator in this environment
  - Same Gradle wrapper / network constraint applies before execution

## 4. Next Test Gaps

### Unit test gaps

- `SubtitleConverter`
  - `STR` 與 `SUB` 共用邏輯的額外案例
  - `timePrecision = false` 的輸出格式
  - 含 HTML tag 的 SRT/VTT 清理
- `StorageHelper`
  - 非 SAF 路徑的保存成功數
  - ZIP 輸出的成功與失敗回報

### Instrumentation next target

1. `MainActivity` 冷啟動不跳額外權限流程
2. SAF 目錄選取後 UI 文案更新
3. 成功轉換後寫出一個檔案到 SAF 目錄

## 5. Notes

- 本文件不再重複手動 QA checklist；裝置驗證與回歸項目統一記錄於 [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md)
- 若後續要補更大範圍的 UI 自動化，請另開新文件，不把本文件擴張成總路線圖
