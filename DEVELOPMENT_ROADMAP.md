# Development Roadmap

本文件整理 LrcApp 目前的開發路線、已完成工作、下一步優先項，以及每一階段的交付標準。

## 1. 目前狀態

### 已完成

#### Phase 1: 邏輯正確性與資料安全

已完成以下修復：

1. 驗證失敗檔案不再與轉換失敗共用狀態
   - 新增 `FileStatus.INVALID`
   - 前置驗證失敗的檔案會保留原始錯誤，不再被直接送進轉換流程
   - 轉換統計改為只計算實際可轉換的檔案

2. ASS/SSA 含逗號文本不再被截斷
   - `SubtitleConverter` 會先解析 `[Events]` 區塊中的 `Format:`
   - 根據 `Text` 欄位位置受控切分欄位，保留歌詞文字中的逗號
   - 補上缺少 `Format:` 時的保守 fallback
   - `cleanAssText()` 額外處理 `\\N` / `\\n` 換行標記

3. SAF 同名輸出不再先刪舊檔
   - `StorageHelper` 會優先重用既有 document
   - 單檔輸出與 ZIP 輸出共用同一策略
   - 成功統計只在實際寫入成功時增加

4. 補上第一批單元測試
   - `FileStatusTest`
   - `SubtitleConverterTest`
   - `StorageHelperTest`

#### Phase 2: Android 11+ 權限與儲存策略整理

已完成以下修復：

1. Android 11+ 匯入流程改為直接使用系統文件選擇器
   - 不再依賴 `MANAGE_EXTERNAL_STORAGE`
   - App 冷啟動時不再先跳出與匯入無關的全域權限流程
2. Android 7~10 保留舊版 `READ_EXTERNAL_STORAGE` 選檔授權
3. Manifest 已移除高風險權限與過時設定
   - 移除 `MANAGE_EXTERNAL_STORAGE`
   - 移除 `requestLegacyExternalStorage`
   - `READ_EXTERNAL_STORAGE` 限制在 `maxSdkVersion=29`
4. App 內文案與文件已對齊
   - 預設輸出位置改稱「預設應用下載目錄」
   - 自訂輸出目錄明確標示為 SAF 授權目錄
   - README 與 Android Studio 指南同步更新

### 已知阻塞

1. 單元測試尚未在此環境實際跑通
   - 原因不是目前變更已確認的編譯錯誤
   - 阻塞點是本機執行環境缺少可直接使用的 Java / Gradle wrapper 下載受限
2. 文件與程式已對齊，但 Android 11+ / Android 7~10 的實機流程仍需要手動驗證

## 2. Roadmap

### Phase 1: 邏輯正確性與資料安全

狀態：`DONE`

交付內容：
- 非法檔案不再進入轉換
- ASS/SSA 含逗號文本不再截斷
- SAF 同名輸出不再先刪舊檔
- 第一批單元測試已補齊

收尾工作：
- 在可用 JDK 與可下載 Gradle wrapper 的環境中跑 `testDebugUnitTest`
- 修正任何因實際編譯或 Android stub 差異暴露出的問題

### Phase 2: Android 11+ 權限與儲存策略整理

狀態：`DONE`

交付內容：
- Android 11+ 使用者不需授予 all files access 也可選檔
- App 冷啟動時不再預先跳全域存取權限流程
- Manifest 與儲存文案對齊 SAF + app-specific downloads 的實際行為
- README 與 Android Studio 指南同步更新

收尾工作：
- 在 Android 11+ 實機或模擬器驗證選檔與輸出流程
- 在 Android 7~10 驗證 `READ_EXTERNAL_STORAGE` 的舊版流程

### Phase 3: 測試補強與回歸保護

狀態：`NEXT`

目標：
- 把目前偏靜態的驗證改成可持續回歸的測試組合

主要工作：
1. 擴充單元測試
   - `FileValidator` 邊界測試
   - `FileNameHelper` 命名清理測試
   - `SubtitleConverter` 的 SRT / VTT / SMI / SUB 基本案例
2. 抽出更易測的純邏輯
   - 盡量讓 parser 與狀態判斷不依賴 Android runtime
3. 規劃最小 instrumentation 測試
   - 至少覆蓋 SAF 寫入主流程或 mockable 包裝層
4. 建立手動 QA 清單
   - 選檔
   - 驗證失敗
   - 轉換成功
   - 同名覆寫
   - 自訂輸出目錄
   - Android 版本差異流程

交付標準：
- 核心 parser 與狀態邏輯有穩定的單元測試
- 重要儲存流程至少有一層自動化保護
- 每次修復後可快速做回歸驗證

### Phase 4: 結構重整與可維護性提升

狀態：`PLANNED`

目標：
- 降低 `MainActivity` 過度承擔的風險
- 讓 UI、流程協調、解析與儲存邏輯更容易維護

主要工作：
1. 拆分 `MainActivity`
   - 將檔案驗證、轉換排程、儲存流程抽成獨立協調層
2. 拆分 parser
   - 將 ASS/SSA、SRT、VTT 等格式解析拆成獨立類別
3. 明確化狀態模型
   - 區分 validation error、conversion error、save error
4. 規劃 ViewBinding 或 UI state 的更乾淨用法

交付標準：
- `MainActivity` 專注 UI 與事件橋接
- parser 與儲存邏輯具備單獨測試能力
- 新增格式或新輸出策略時不需要大改主流程

## 3. 建議近期優先順序

### 立即進行

1. 在有可用 JDK 與網路的環境跑通 `testDebugUnitTest`
2. 補齊 Phase 3 的 parser / validator / storage 測試
3. 做 Android 11+ 與 Android 7~10 的實機手動驗證

### 之後接續

1. 重整 `MainActivity` 的責任分配
2. 規劃 parser 與儲存層拆分

## 4. 驗收清單

### Phase 1 驗收

- 不支援格式與超大檔案維持 `INVALID`
- ASS/SSA 含逗號文本完整輸出
- SAF 同名覆寫不再先刪檔
- 測試在正式開發環境可跑通

### Phase 2 驗收

- Android 11+ 不再阻塞於 all files access
- 選檔與輸出全流程正常
- 文件與產品行為一致

### Phase 3 驗收

- 單元測試覆蓋主要 parser 與 validator
- 儲存路徑至少有一層自動化驗證
- 重要 Android 版本差異流程有手動驗證紀錄

## 5. 相關文件

- [Review Findings Plan](./REVIEW_FINDINGS_PLAN.md)
- [README](./README.md)
- [Android Studio Guide](./ANDROID_STUDIO_GUIDE.md)
