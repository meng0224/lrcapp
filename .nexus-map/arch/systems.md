> generated_by: nexus-mapper v2
> verified_at: 2026-03-18
> provenance: AST-backed for Kotlin file/class inventory; system boundaries are validated with README, hotspot data, and manual import inspection because query_graph did not recover internal Kotlin import edges here.

# System Boundaries

## UI Orchestrator

- code_path: `app/src/main/java/com/example/lrcapp/MainActivity.kt`
- supporting UI files: `app/src/main/java/com/example/lrcapp/adapter/SubtitleFileAdapter.kt`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/layout/item_subtitle_file.xml`
- responsibility: 管理選檔、遞迴掃描、權限請求、批次轉換、保存結果回饋與整體畫面狀態。
- evidence: `MainActivity` 長達 1100+ 行，直接 import converter、model、adapter 與多個 util helper；git hotspot 排名第 1。
- risk: 高。需求一旦牽涉授權、輸出模式或列表行為，通常都會回到這個檔案。

## Conversion Engine

- code_path: `app/src/main/java/com/example/lrcapp/converter/`
- primary file: `app/src/main/java/com/example/lrcapp/converter/SubtitleConverter.kt`
- responsibility: 讀取字幕內容，依副檔名選擇解析器，清理文本並輸出 LRC 時間標籤。
- evidence: README 的格式支援列表與 `SubtitleConverterTest.kt` 對 `ASS/SRT/VTT/SMI/SUB` 的直接驗證一致。
- risk: 中。檔案變更頻率不高，但屬功能正確性的核心。

## Storage And Policy Services

- code_path: `app/src/main/java/com/example/lrcapp/util/`
- notable files:
  - `StorageHelper.kt`: SAF/tree URI 寫入、相對路徑重建、下載目錄落地、檔名驗證。
  - `SettingsManager.kt`: SharedPreferences 載入與保存輸出/遞迴匯入設定，以及來源/匯入根目錄授權 URI。
  - `FileValidator.kt`, `FileNameHelper.kt`, `FileSelectionPolicy.kt`, `OutputSettingsPolicy.kt`, `FileListUiPolicy.kt`: 純策略 helper。
- responsibility: 承接 UI 之外的驗證、命名、列表規則、設定持久化與實際輸出行為。
- evidence: git hotspot 第 3 名為 `StorageHelper.kt`，且它與 `SettingsManager.kt`、`MainActivity.kt` 有高 co-change。
- risk: 中到高。這層影響 SAF 保存成功率、權限延續與重複匯入規則。

## File State Domain

- code_path: `app/src/main/java/com/example/lrcapp/model/`
- files: `AppSettings.kt`, `SubtitleFile.kt`
- responsibility: 定義檔案狀態、輸出內容暫存、來源/匯入根目錄上下文與使用者設定。
- evidence: `MainActivity.kt` 與多個 util helper 共用這些資料結構；git coupling 也顯示它們與 UI/Storage 同步變動。
- risk: 低到中。結構簡單，但一旦欄位調整會擴散到 UI、保存與測試。

## Boundary Validation Notes

- Q1 validated: `MainActivity.kt` 確實是主入口，而不只是薄 UI。它同時包含 import、scan、conversion、authorization、save、UI refresh 等 40+ 方法。
- Q2 validated: `StorageHelper.kt` 是保存邊界的真核心，而不是一般 helper。它負責 DocumentFile 建立、寫入驗證與輸出結果判定。
- Q3 validated: `util/` 雖然名稱通用，但這個 repo 中它實際承載了多數業務策略，不應當成可忽略的雜項目錄。
