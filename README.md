# LrcApp

一個功能完整的 Android 字幕轉換應用程式，支持將多種字幕格式（VTT, ASS, SSA, SRT, STR, SMI, SUB）轉換為 LRC 格式。

## 主要功能

### 📁 文件導入模塊
- **系統文件選擇器集成**：使用 Android 原生文件管理器，支持批量選擇
- **格式過濾**：自動過濾並僅顯示支持的副檔名（.vtt, .ass, .ssa, .srt, .str, .smi, .sub）
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
  - Android 10 以下：處理 READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE 權限
  - Android 10+：利用 Scoped Storage 機制
- **文件編碼**：UTF-8 編碼，確保多國語言（中、日、韓等）不亂碼
- **離線運行**：所有解析邏輯與資源均內置，無需網絡連接

## 專案結構

```
lrcapp/
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
├── gradle.properties
└── .gitignore
```

## 技術規格

- **語言**: Kotlin
- **最低 SDK**: 24 (Android 7.0)
- **目標 SDK**: 34 (Android 14)
- **構建工具**: Gradle 8.2.0
- **Kotlin 版本**: 1.9.22

## 依賴項

- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- ConstraintLayout
- RecyclerView
- Lifecycle Runtime KTX
- Activity KTX
- Kotlin Coroutines

## 開發環境要求

- Android Studio Hedgehog 或更新版本
- JDK 8 或更高版本
- Android SDK 34

## 使用流程

1. 啟動應用，進入主界面
2. 點擊「選擇文件」，調出系統文件選擇器
3. 選中一個或多個字幕文件，應用解析並填入列表
4. （可選）切換「智能命名」或「時間精度」開關
5. 點擊「開始轉換」
6. 界面顯示進度條，直至 100%
7. 結果區自動展開，列出所有生成文件
8. 點擊「打包下載」或「直接下載」保存文件

## 構建專案

```bash
./gradlew build
```

## 安裝到設備

```bash
./gradlew installDebug
```

## 注意事項

- 文件大小限制：單個文件不能超過 10MB
- 支持的格式：VTT, ASS, SSA, SRT, STR, SMI, SUB
- 輸出格式：LRC（UTF-8 編碼）
- 文件保存位置：應用專屬目錄或系統 Download 目錄（根據 Android 版本）
