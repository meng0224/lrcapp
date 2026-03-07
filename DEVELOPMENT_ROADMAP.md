# Development Roadmap

本文件是 LrcApp 的唯一主路線圖，負責回答三件事：
- 做了什麼
- 現在在哪
- 接下來做什麼

其餘開發文件分工如下：
- 歷史 / 決策背景：[REVIEW_FINDINGS_PLAN.md](./REVIEW_FINDINGS_PLAN.md)
- Phase 2 驗證執行：[PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md)
- Phase 3 測試策略與執行鏈路：[PHASE3_TEST_HARDENING.md](./PHASE3_TEST_HARDENING.md)

## 1. Current Status

### Phase 1: 邏輯正確性與資料安全

- 狀態：`DONE`
- 目標：修正資料正確性與 SAF 覆寫風險
- 已完成：
  - 驗證失敗檔案改為 `INVALID`
  - ASS/SSA 含逗號文本不再截斷
  - SAF 同名輸出不再先刪舊檔
  - 第一批單元測試已補上
- 未完成：
  - 在可用環境實際跑通 unit tests
- 下一步：
  - 在外部可用 Gradle 環境執行 `testDebugUnitTest`

### Phase 2: Android 11+ 權限與儲存策略整理

- 狀態：`VALIDATION PENDING`
- 目標：移除 `MANAGE_EXTERNAL_STORAGE` 依賴，改以 SAF 為主
- 已完成：
  - Android 11+ 改為直接走系統文件選擇器
  - 冷啟動不再主動跳全域權限流程
  - Manifest 移除高風險權限與過時設定
  - README / guide / UI 文案已對齊
- 未完成：
  - Android 11+ / Android 7~10 的實機或模擬器驗證結果尚未回填
- 下一步：
  - 依 [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md) 完成驗證與回填

### Phase 3: 測試補強與回歸保護

- 狀態：`IN PROGRESS`
- 目標：把已修好的核心邏輯變成可持續回歸的測試資產
- 已完成：
  - `FileValidatorTest`
  - `FileNameHelperTest`
  - 擴充 `SubtitleConverterTest`
  - `StorageHelper.countSuccessfulResults()` 可測 helper
  - 最小 instrumentation 測試 `MainActivityInstrumentationTest`
  - 測試策略文件與驗證報告模板
- 未完成：
  - Unit tests 尚未在此環境跑通
  - Instrumentation tests 尚未執行
  - Phase 2 驗證結果尚未併入測試執行結論
- 下一步：
  - 先在可用環境跑 `testDebugUnitTest`
  - 再跑 `connectedDebugAndroidTest`

### Phase 4: 結構重整與可維護性提升

- 狀態：`PLANNED`
- 目標：降低 `MainActivity` 的耦合度，提升 parser / storage / flow 的可維護性
- 已完成：
  - 無
- 未完成：
  - `MainActivity` 職責拆分
  - parser 類別拆分
  - 更清楚的錯誤模型
- 下一步：
  - 等 Phase 2 驗證與 Phase 3 自動化穩定後再開始

## 2. Feature Track

### 輸出到原文件目錄

- 狀態：`IN PROGRESS`
- 目標：新增可持久化的輸出模式，讓轉換結果可依來源資料夾寫回原文件目錄
- 已完成：
  - `AppSettings` / `SettingsManager` 新增 `outputToSourceDirectory`
  - 主畫面新增「輸出到原文件目錄」開關與「清除目錄」按鈕
  - 自訂輸出資料夾與原文件目錄模式互斥
  - 每個來源資料夾可保存獨立的 SAF tree URI 授權
  - `StorageHelper` 新增多目標輸出模型與保存結果統計
  - 補上 `SettingsManagerTest`、`OutputSettingsPolicyTest`、`StorageHelperTest` 擴充案例
- 未完成：
  - 在可用環境實際編譯並跑通測試
  - 實機驗證單一來源 / 多來源 / 重用授權流程
- 下一步：
  - 在 Android 11+ 裝置驗證逐目錄授權與回寫流程
  - 在可用 Gradle 環境執行 unit tests 與回歸檢查

## 3. Current Blockers

- `gradlew.bat testDebugUnitTest` 受限於目前環境無法下載 `gradle-9.0-milestone-1-bin.zip`
- instrumentation 需要可用 emulator / device
- Phase 2 雖已實作，但尚未完成裝置驗證回填
- 新增的「輸出到原文件目錄」功能尚未在實機上完成逐目錄授權驗證

## 4. Immediate Next Steps

1. 在可提供 Gradle 發行版的環境執行 unit tests
2. 在可用 emulator / device 執行 instrumentation tests
3. 依 validation report 完成 Android 11+ 與 Android 7~10 驗證
4. 針對「輸出到原文件目錄」驗證單一來源、多來源與授權重用流程
5. 更新各附屬文件中的 `status / passed / blocked` 結果

## 5. Acceptance Snapshot

- Phase 1 完成條件：功能修復已上線且 unit tests 在正式環境跑通
- Phase 2 完成條件：Android 11+ 與 Android 7~10 驗證結果已回填且通過
- Phase 3 完成條件：unit tests 與最小 instrumentation 至少各跑通一次
- Phase 4 開始條件：Phase 2、3 不再處於 `VALIDATION PENDING` / `IN PROGRESS`
- 新功能完成條件：來源目錄輸出在單一來源、多來源與重複授權重用場景都通過手動驗證
