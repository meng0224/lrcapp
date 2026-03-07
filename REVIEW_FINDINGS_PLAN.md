# Review Findings Follow-up Plan

本文件整理目前 code review 的 4 個 findings，目標是把修復工作拆成可執行、可驗證、可分階段落地的後續路線。

## 1. 概覽

### Findings 清單

1. `MainActivity.kt`: Android 11+ 被強制要求 `MANAGE_EXTERNAL_STORAGE`，阻塞其實已可由 SAF 完成的流程
2. `MainActivity.kt`: 驗證失敗的檔案仍會再次進入轉換流程
3. `SubtitleConverter.kt`: ASS/SSA 在歌詞文本含逗號時會被截斷
4. `StorageHelper.kt`: SAF 輸出時先刪除舊檔，若後續建立或寫入失敗會造成資料遺失

### 修復優先順序

1. `P1` 資料正確性與資料安全
   - Finding 2
   - Finding 3
   - Finding 4
2. `P2` 平台權限與產品可用性
   - Finding 1

### 建議交付節奏

1. 第一批修復
   - 先處理 Finding 2、3、4
   - 原因是這三項直接影響輸出內容正確性、使用者已有檔案的安全性，以及實際轉換結果
2. 第二批修復
   - 再處理 Finding 1
   - 需要一併調整權限策略、Manifest 宣告與 README 行為說明
3. 第三批補強
   - 補單元測試與最少量 UI/整合驗證
   - 補文件與回歸檢查清單

## 2. 各 Finding 詳細規劃

### Finding 1: Android 11+ 被不必要地強制要求 All files access

#### 目標

讓 Android 11 以上裝置在不授予 `MANAGE_EXTERNAL_STORAGE` 的情況下，仍能正常：
- 開啟字幕檔案選擇器
- 選擇輸出資料夾
- 讀取所選文件
- 寫入使用者授權的目錄

#### 問題根因

- `checkAndRequestPermissions()` 將 Android 11+ 的主流程前置條件綁定到 `Environment.isExternalStorageManager()`
- 但實際檔案讀寫使用的是 `OpenMultipleDocuments` 與 `OpenDocumentTree`
- 這兩個 SAF API 本身已提供受限授權，不需要 `MANAGE_EXTERNAL_STORAGE`

#### 實作策略

1. 調整 `MainActivity.checkAndRequestPermissions()`
   - Android 11+ 不再為了開啟文件選擇器要求 `MANAGE_EXTERNAL_STORAGE`
   - 對 `OpenMultipleDocuments` 流程直接開啟檔案選擇器
2. 重新界定權限檢查責任
   - 一般匯入與匯出以 SAF 授權為主
   - 保留舊式儲存權限邏輯僅在確實需要直接存取公用檔案系統時才使用
3. 清理 Manifest
   - 移除或評估移除 `MANAGE_EXTERNAL_STORAGE`
   - 檢查 `requestLegacyExternalStorage` 是否仍有保留必要
4. 更新說明文件
   - README 與教學文件中的權限說明要與實作一致

#### 驗證方式

- Android 11+ 首次啟動時，不要求 all files access 也可選檔
- 使用者選定輸出目錄後可成功保存 LRC
- Android 7~10 流程不退化
- 若未選輸出目錄，預設儲存邏輯仍正常

#### 風險與注意事項

- 若仍保留非 SAF 的預設輸出行為，需再次確認該路徑在 Android 11+ 的行為是否與 UI 文案一致
- 權限調整會連帶影響 README、商店政策風險與 QA 測試腳本

### Finding 2: 驗證失敗檔案仍被送進轉換流程

#### 目標

讓「前置驗證失敗」與「轉換過程失敗」成為兩種分離的狀態，避免無效檔案再次被處理，也避免覆寫原始錯誤訊息。

#### 問題根因

- `startConversion()` 使用 `PENDING || ERROR` 當作可轉換條件
- 但 `ERROR` 狀態同時代表：
  - 檔案前置驗證失敗
  - 真正轉換失敗
- 狀態語意混在一起，導致不該重試的項目也被重跑

#### 實作策略

1. 最小修復方案
   - 將 `filesToProcess` 改為只處理 `PENDING`
   - 保留驗證失敗的原始錯誤訊息
2. 較完整方案
   - 將 `FileStatus` 拆分為更明確的狀態，例如：
     - `PENDING`
     - `INVALID`
     - `PROCESSING`
     - `SUCCESS`
     - `ERROR`
   - `INVALID` 專門表示副檔名/大小檢查未通過
   - `ERROR` 僅表示解析或寫出失敗
3. 同步調整 UI
   - Adapter 顯示對應狀態文案
   - 統計成功/失敗時，不把 `INVALID` 當成「已嘗試轉換但失敗」
4. 更新使用者提示
   - 完成 Toast 應反映實際被處理檔案數

#### 驗證方式

- 不支援格式檔案在選取後維持原始錯誤，不進入轉換
- 大於 10MB 檔案不會被讀入內容
- Toast 中的總數只計算實際進入轉換的項目
- 手動製造解析失敗案例時，仍可正確顯示轉換失敗

#### 風險與注意事項

- 一旦拆出 `INVALID`，需要同步更新 Adapter 顯示與任何狀態統計邏輯
- 若日後要支援「重試失敗檔案」，應只允許重試 `ERROR` 而非 `INVALID`

### Finding 3: ASS/SSA 文字欄位遇到逗號會被截斷

#### 目標

讓 ASS/SSA 的 `Dialogue:` 解析能正確保留文本中的逗號，不再產生截斷歌詞的錯誤輸出。

#### 問題根因

- 現行實作以 `lastIndexOf(',')` 取得最後一欄
- 這假設了「最後一個逗號之後的內容才是文字」
- 但 ASS/SSA 的文字欄位本身可以合法包含逗號

#### 實作策略

1. 解析 `Format:` 行
   - 在 `[Events]` 區塊讀取 `Format:` 欄位順序
   - 找出 `Text` 欄位位置
2. 依欄位數做受控切分
   - 只切前 `n-1` 個逗號，剩下整段視為 `Text`
   - 例如使用限制次數的 split，或手寫欄位解析
3. 保留既有清理行為
   - 解析出文本後仍走 `cleanAssText()`
4. 加強相容性
   - 若 `Format:` 缺失，提供保守 fallback
   - fallback 不能再用 `lastIndexOf(',')`
   - 建議改以 ASS 預設前 9 欄推估，將其後剩餘字串視為文字欄

#### 驗證方式

- `Dialogue: ...,Hello, world` 應輸出完整 `Hello, world`
- 帶有樣式碼的文本仍能清理乾淨
- 不同 `Format:` 欄位順序下仍能抓到正確 `Text`
- 無 `Format:` 的簡化 ASS 檔案仍能維持可用

#### 風險與注意事項

- ASS/SSA 的 parser 一旦變嚴格，需要注意不要誤傷既有簡化格式
- 若未來要支援更完整 ASS 能力，建議把 ASS/SSA 解析獨立成專屬 parser 類別

### Finding 4: SAF 覆蓋寫入先刪舊檔，存在資料遺失風險

#### 目標

確保使用 SAF 寫入時，即使建立新檔或輸出流失敗，也不會先破壞使用者原本已有的輸出檔案。

#### 問題根因

- `saveContentToUri()` 先 `findFile()` 再 `delete()`
- 只要後續 `createFile()` 或 `openOutputStream()` 失敗，就會遺失原檔
- `saveAsZip()` 也重複了相同模式

#### 實作策略

1. 優先方案：覆寫既有文件內容
   - 若 `findFile(fileName)` 找到現有檔案，直接對該 URI 開啟輸出流寫入
   - 避免先刪除再重建
2. 次佳方案：暫存檔替換
   - 先寫入暫存名稱
   - 寫入成功後再刪除舊檔並重新命名或保留新檔
   - 但 SAF 下 rename 支援度較不一致，優先採直接覆寫
3. 將修復同步套用到
   - `saveContentToUri()`
   - `saveAsZip()`
4. 補保存結果回報
   - 若單檔保存失敗，應能回傳失敗並避免假性成功統計

#### 驗證方式

- 目標目錄已有同名檔案時，重寫成功且不丟失資料
- 建立新檔失敗時，原檔仍存在
- ZIP 匯出與單檔匯出都套用一致策略
- 多檔保存時，成功數量與實際結果一致

#### 風險與注意事項

- `ContentResolver.openOutputStream(uri)` 的 mode 若依 provider 不同而有差異，需要確認是否會 truncate 舊內容
- 若 provider 不支援穩定覆寫，需針對失敗情境保留清楚錯誤訊息

## 3. 建議的實作順序

### Phase 1: 邏輯正確性與資料安全

先完成：
- Finding 2
- Finding 3
- Finding 4

交付標準：
- 非法檔案不再進入轉換
- ASS/SSA 含逗號文本不再截斷
- SAF 同名輸出不再先刪舊檔

### Phase 2: 平台權限與文件同步

再完成：
- Finding 1

交付標準：
- Android 11+ 不再阻塞於 all files access
- README 與實際權限流程一致
- Manifest 權限宣告合理化

### Phase 3: 測試與回歸保護

補上最低限度測試：
- `SubtitleConverter` 單元測試
- `FileValidator` / `FileNameHelper` 單元測試
- `StorageHelper` 可測邏輯抽離或最小化 instrumentation 驗證
- 手動 QA 清單

## 4. 測試計畫

### 單元測試

- `FileValidator`
  - 支援格式與不支援格式
  - 邊界大小 `10MB` 與 `10MB + 1`
- `FileNameHelper`
  - 一般副檔名轉 `.lrc`
  - `music.mp3.ass` 轉成 `music.lrc`
- `SubtitleConverter`
  - SRT 基本轉換
  - VTT 多行文字合併
  - ASS/SSA 文本含逗號
  - ASS/SSA 含樣式碼
  - 空內容與不支援格式

### 手動測試

- Android 11+ 不授予 all files access 仍可選檔與輸出
- 選入不支援格式時，列表維持驗證錯誤
- 選入超大檔案時，不進入轉換
- 同名檔案覆蓋輸出不丟資料
- 選擇 SAF 輸出目錄後，重啟 App 仍保有輸出位置設定

### 回歸重點

- 既有 SRT/VTT 正常案例不退化
- 預設輸出路徑仍可保存
- 成功/失敗統計與進度條顯示不失真

## 5. 建議產出

實作完成後應同步交付：

1. 修復程式碼
2. 測試案例
3. README 權限與儲存說明更新
4. 一份簡短 changelog 或修復摘要

## 6. 備註

如果要降低一次改動的風險，建議先做最小正確性修復：

1. `filesToProcess` 只處理 `PENDING`
2. ASS/SSA 改為可保留文字欄中的逗號
3. SAF 優先覆寫既有檔案，不先刪除

等第一批穩定後，再進行 Android 11+ 權限策略調整與文件同步，會比較容易驗證回歸範圍。
