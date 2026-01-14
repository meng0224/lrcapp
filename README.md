# LrcApp

一個功能完整的 Android 字幕轉換應用程式，支持將多種字幕格式（VTT, ASS, SSA, SRT, STR, SMI, SUB）轉換為 LRC 格式。

## Android Studio 安裝與使用教學

請先閱讀：`ANDROID_STUDIO_GUIDE.md`

## 主要功能

### 📁 文件導入模塊
- **系統文件選擇器集成**：使用 Android 原生文件管理器，支持批量選擇
- **格式過濾（現況）**：目前選擇器以 `*/*` 顯示所有檔案；選取後會再用副檔名做格式驗證（未在選擇器內預先過濾）
- **前置校驗**：
  - 格式校驗：檢查文件副檔名，不匹配則標記錯誤
  - 體積校驗：超過 10MB 的文件將被攔截並提示

### ⚙️ 應用設置模塊
- **智能命名清理**：自動移除文件名中的媒體副檔名（如 music.mp3.ass → music.lrc）
- **時間精度優化**：保留毫秒/百分秒精度，確保 LRC 歌詞播放時的時間同步（默認開啟）

### 🔄 核心轉換引擎
- **多格式解析器**：
  - VTT 解析：讀取 WebVTT 標準
  - SRT 解析：支持逗號與點號分隔的毫秒數
  - ASS/SSA 解析：提取 Dialogue 標籤內容
  - SMI 解析：解析微軟 SMI 格式，提取 `<SYNC>` 時間節點
  - SubViewer 解析：解析 SUB 格式
- **數據清洗機制**：
  - 樣式過濾：自動移除 ASS/SSA 中的樣式代碼
  - 標籤剝離：移除 VTT/SRT 中的 HTML 標籤
  - 換行處理：將多行文本合併為單行
- **時間軸重構**：統一轉換為 LRC 標準格式 `[mm:ss.xx]`

### 📊 進度與反饋機制
- **實時進度條**：根據已處理文件數量動態更新百分比
- **狀態通知**：Toast 提示操作結果
- **結果列表**：
  - 成功：顯示綠色標記與輸出文件名
  - 失敗：顯示紅色標記與具體失敗原因

### 💾 文件導出模塊
- **單個導出**：每個轉換成功的文件可單獨下載
- **批量壓縮導出**：將所有成功轉換的文件打包為 ZIP（自動命名，附加時間戳）
- **直接批量導出**：連續寫入多個文件，不進行壓縮

### 🔐 數據存儲與兼容性
- **權限處理**：
  - Android 7~10：使用 `READ_EXTERNAL_STORAGE`（並在 Manifest 仍保留舊式寫入相關宣告）
  - Android 11+（現況）：會引導使用者授予「所有檔案存取權」（`MANAGE_EXTERNAL_STORAGE`），以便順利讀取/寫入
- **輸出位置**：
  - 預設：寫入 App 外部檔案下載目錄（App-specific downloads）
  - 可選：可用「選擇輸出資料夾」改用 SAF（`OpenDocumentTree`）寫入指定資料夾（會保存 URI 權限）
- **文件編碼**：UTF-8 編碼，確保多國語言（中、日、韓等）不亂碼
- **離線運行**：所有解析邏輯與資源均內置，無需網絡連接

## 專案結構

```
lrcapp/
├── ANDROID_STUDIO_GUIDE.md        # Android Studio 安裝與使用教學
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/lrcapp/
│       │   ├── MainActivity.kt              # 主活動
│       │   ├── adapter/
│       │   │   └── SubtitleFileAdapter.kt    # RecyclerView 適配器
│       │   ├── converter/
│       │   │   └── SubtitleConverter.kt     # 核心轉換引擎
│       │   ├── model/
│       │   │   ├── SubtitleFile.kt          # 字幕文件數據模型
│       │   │   └── AppSettings.kt           # 應用設置模型
│       │   └── util/
│       │       ├── FileValidator.kt         # 文件驗證工具
│       │       ├── FileNameHelper.kt        # 文件名處理工具
│       │       ├── StorageHelper.kt         # 存儲工具
│       │       └── SettingsManager.kt       # 設置管理工具
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml        # 主界面布局
│           │   └── item_subtitle_file.xml   # 文件列表項布局
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle
├── settings.gradle
├── gradle/                       # Gradle Wrapper
├── gradlew
├── gradlew.bat
├── gradle.properties
├── local.properties              # 本機 SDK 路徑（通常不提交）
└── .gitignore
```

## 技術規格

- **語言**: Kotlin
- **Compile SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Android Gradle Plugin (AGP)**: 8.2.0
- **Gradle Wrapper**: 9.0-milestone-1
- **Kotlin 版本**: 1.9.22

## 依賴項

- AndroidX Core KTX (`androidx.core:core-ktx`)
- AndroidX AppCompat (`androidx.appcompat:appcompat`)
- Material Design Components (`com.google.android.material:material`)
- ConstraintLayout (`androidx.constraintlayout:constraintlayout`)
- RecyclerView (`androidx.recyclerview:recyclerview`)
- Lifecycle Runtime KTX (`androidx.lifecycle:lifecycle-runtime-ktx`)
- Activity KTX (`androidx.activity:activity-ktx`)
- DocumentFile / SAF (`androidx.documentfile:documentfile`)
- Kotlin Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android`)

## 開發環境要求

- Android Studio Hedgehog 或更新版本
- **JDK 17**（AGP 8.2.x 建議/常用需求；Android Studio 內建 JBR 也可）
- Android SDK Platform 34

## 快速開始（建議）

1. 用 Android Studio 開啟專案根目錄（包含 `settings.gradle` 的資料夾）。
2. 等待 Gradle Sync 完成。
3. 建立/啟動模擬器或連接實體手機（需開啟 USB 偵錯）。
4. 點擊工具列的 Run（▶）運行 `app`。

## 使用流程

1. 啟動應用，進入主界面
2.（可能出現）依 Android 版本要求授予讀取/存取權限  
   - Android 11+：可能會引導到系統頁面授予「所有檔案存取權」
   - Android 7~10：可能會跳出讀取外部儲存權限請求
3.（可選）點擊「選擇目錄」設定輸出資料夾（SAF），未設定則使用預設下載目錄
4. 點擊「選擇文件」，調出系統文件選擇器並一次選取多個字幕檔
5. 選取完成後，App 會先做前置校驗：不支援格式或超過 10MB 會直接標記「錯誤」
6.（可選）切換「智能命名清理」或「時間精度優化」開關
7. 點擊「開始轉換」，列表狀態會依序顯示「處理中 / 成功 / 錯誤」，並顯示進度百分比
8. 轉換成功後：
   - 單檔：點擊該列的「下載」
   - 批次：點擊「打包下載」（ZIP）或「直接下載」（逐檔寫入）

## 構建專案

### Windows（PowerShell / CMD）

```bash
.\gradlew.bat build
```

### macOS / Linux

```bash
./gradlew build
```

## 安裝到設備

### Windows（PowerShell / CMD）

```bash
.\gradlew.bat installDebug
```

### macOS / Linux

```bash
./gradlew installDebug
```

## 注意事項

- 文件大小限制：單個文件不能超過 10MB
- 支持的格式：VTT, ASS, SSA, SRT, STR, SMI, SUB
- 輸出格式：LRC（UTF-8 編碼）
- 文件保存位置：預設寫入 App 外部檔案下載目錄；或由使用者在 App 內選擇輸出資料夾後寫入

## 已知限制（與目前程式一致）

- **選擇器副檔名過濾**：目前未在系統選擇器中限制可見檔案，會在選取後才做副檔名驗證。
- **Android 11+ 權限策略**：目前採用「所有檔案存取權」的方式簡化流程；若要更貼近 Scoped Storage 最佳實務，可再改為完全依賴 SAF + MediaStore。
- **字幕解析覆蓋範圍**：轉換器以常見格式規則實作，對於某些複雜情況（例如 VTT 多行 cue、ASS 逗號分隔更複雜的 Dialogue 欄位）可能需要進一步強化。
