# Phase 3 Test Hardening

本文件整理目前 Phase 3 的測試補強範圍，包含自動化測試、最小 instrumentation 驗證範圍，以及手動 QA checklist。

## 1. Unit Test Coverage

### 已補上的測試

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

### 下一步可擴充的 unit tests

- `SubtitleConverter`
  - `STR` 與 `SUB` 共用邏輯的額外案例
  - `timePrecision = false` 的輸出格式
  - 含 HTML tag 的 SRT/VTT 清理
- `StorageHelper`
  - 非 SAF 路徑的保存成功數
  - ZIP 輸出的成功與失敗回報

## 2. Minimal Instrumentation Scope

這一輪不追求完整 UI 自動化，先定義最小可行驗證路徑：

1. `MainActivity` 在 Android 11+ 冷啟動時不主動跳全域權限流程
2. 選定 SAF 目錄後，可取得 persistable URI permission 並更新輸出位置顯示
3. 若條件允許，再補一條「成功轉換後寫出一個檔案到 SAF 目錄」的整合驗證

### 建議落地順序

1. 先做 `MainActivity` 冷啟動不跳額外權限的驗證
2. 再做 SAF 目錄選取後 UI 文案更新的驗證
3. 最後才考慮實際寫檔的 instrumentation

## 3. Manual QA Checklist

### Android 11+

- 冷啟動 App
  - 不出現 all-files-access 導頁
- 點選「選擇文件」
  - 可直接開啟系統文件選擇器
- 選擇一個合法 SRT/VTT/ASS 檔案
  - 檔案進入 `待處理`
- 點選「選擇目錄」並選取 SAF 目錄
  - UI 顯示 `SAF 授權目錄`
- 點選「開始轉換」
  - 成功完成轉換與保存
- 重新轉換同名檔案
  - 不先刪掉舊檔

### Android 7~10

- 冷啟動 App
  - 不主動請求權限
- 點選「選擇文件」
  - 第一次會要求 `READ_EXTERNAL_STORAGE`
- 授權後再次選檔
  - 可正常開啟系統文件選擇器
- 完成一次合法檔案轉換
  - 可正常保存到預設應用下載目錄或 SAF 目錄

### 驗證與回歸

- 選入 `.txt` 或其他不支援格式
  - 狀態維持 `無效`
- 選入超過 `10MB` 檔案
  - 狀態維持 `無效`
- ASS/SSA 文本含逗號
  - 輸出內容不截斷
- SRT/VTT 含多行文字
  - 合併為單行輸出

## 4. Environment Notes

目前這個環境尚未跑通 `testDebugUnitTest`，阻塞點為：

- `JAVA_HOME` 需要指到可用 JDK / JBR
- Gradle wrapper 下載受網路限制影響

因此本階段交付包含：
- 測試檔與可測 helper API 已補齊
- instrumentation 範圍已定義
- 手動 QA checklist 已整理
