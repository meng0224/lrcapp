# LrcApp

一個功能完整的 Android 字幕轉換應用程式，支持將多種字幕格式（VTT, ASS, SSA, SRT, STR, SMI, SUB）轉換為 LRC 格式。

## 開發文件索引

- [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md) - 主開發路線圖與目前狀態
- [REVIEW_FINDINGS_PLAN.md](./REVIEW_FINDINGS_PLAN.md) - code review findings 的歷史決策背景
- [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md) - Android 版本差異與 Phase 2 驗證記錄
- [PHASE3_TEST_HARDENING.md](./PHASE3_TEST_HARDENING.md) - 測試補強、執行鏈路與阻塞
## Android Studio 安裝與使用教學

請先閱讀：`ANDROID_STUDIO_GUIDE.md`

## 主要功能

### 📁 文件導入模塊
- **系統文件選擇器集成**：使用 Android 原生文件管理器（`OpenMultipleDocuments`），支持批量選擇
- **格式過濾（現況）**：目前選擇器以 `*/*` 顯示所有檔案；選取後會再用副檔名做格式驗證（未在選擇器內預先過濾）
- **前置校驗**：
  - 格式校驗：檢查文件副檔名，不匹配則標記「錯誤」狀態並顯示「不支持的格式」
  - 體積校驗：超過 10MB 的文件將被標記「錯誤」狀態並顯示「文件過大（超過 10MB）」
- **文件列表管理**：選擇新文件時會**清空舊列表**，替換為新選擇的文件

### ⚙️ 應用設置模塊
- **智能命名清理**：自動移除文件名中的媒體副檔名（如 music.mp3.ass → music.lrc），**預設啟用**（目前代碼中硬編碼為 `true`）
- **時間精度優化**：保留毫秒/百分秒精度，確保 LRC 歌詞播放時的時間同步，**預設啟用**（目前代碼中硬編碼為 `true`）
- **輸出目錄選擇**：可選擇自訂輸出資料夾（使用 SAF），未選擇則使用預設應用下載目錄（App-specific downloads）
- **原文件目錄輸出**：可開啟「輸出到原文件目錄」，轉換後依來源資料夾逐一要求 SAF 授權並寫回原目錄

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
- **實時進度條**：使用 `LinearProgressIndicator`，根據已處理文件數量動態更新百分比（0-100%）
- **狀態通知**：Toast 提示操作結果（如「已選擇 X 個文件」、「轉換完成！成功: X / Y」等）
- **結果列表**：
  - 待處理：灰色標記
  - 處理中：藍色標記
  - 成功：綠色標記與輸出文件名
  - 失敗：紅色標記與具體失敗原因

### 💾 文件導出模塊
- **自動保存**：轉換完成後，所有成功轉換的文件會**自動保存**到指定的輸出目錄（預設、自訂 SAF 目錄，或已授權的原文件目錄）
- **批量保存**：使用 `saveMultipleFiles()` 方法，連續寫入多個 LRC 文件，不進行壓縮
- **保存結果提示**：顯示已保存的文件數量

### 🔐 數據存儲與兼容性
- **權限處理**：
  - Android 7~10：使用 `READ_EXTERNAL_STORAGE`（並在 Manifest 仍保留舊式寫入相關宣告）
  - Android 11+：檔案匯入、自訂輸出目錄與原文件目錄輸出都依賴 SAF 授權，不要求「所有檔案存取權」
- **輸出位置**：
  - 預設：寫入 App 外部檔案下載目錄（App-specific downloads）
  - 可選：可用「選擇輸出資料夾」改用 SAF（`OpenDocumentTree`）寫入指定資料夾（會保存 URI 權限）
  - 原文件目錄模式：開啟「輸出到原文件目錄」後，會對每個來源資料夾逐一要求 SAF 授權並保存對應 tree URI
- **文件編碼**：UTF-8 編碼，確保多國語言（中、日、韓等）不亂碼
- **離線運行**：所有解析邏輯與資源均內置，無需網絡連接

## 專案結構

```
lrcapp/
├── ANDROID_STUDIO_GUIDE.md        # Android Studio 安裝與使用教學
├── README.md                      # 專案說明文件
├── app/
│   ├── build.gradle               # App 模組的 Gradle 配置
│   ├── proguard-rules.pro         # ProGuard 混淆規則
│   └── src/main/
│       ├── AndroidManifest.xml   # Android 應用清單
│       ├── ic_launcher-playstore.png  # Play Store 圖標
│       ├── java/com/example/lrcapp/
│       │   ├── MainActivity.kt              # 主活動（UI 控制與業務邏輯）
│       │   ├── adapter/
│       │   │   └── SubtitleFileAdapter.kt   # RecyclerView 適配器（文件列表顯示）
│       │   ├── converter/
│       │   │   └── SubtitleConverter.kt    # 核心轉換引擎（多格式解析與轉換）
│       │   ├── model/
│       │   │   ├── SubtitleFile.kt          # 字幕文件數據模型
│       │   │   └── AppSettings.kt           # 應用設置模型
│       │   └── util/
│       │       ├── FileValidator.kt         # 文件驗證工具（格式與大小檢查）
│       │       ├── FileNameHelper.kt        # 文件名處理工具（智能命名）
│       │       ├── StorageHelper.kt          # 存儲工具（文件保存與 ZIP 壓縮）
│       │       └── SettingsManager.kt       # 設置管理工具（SharedPreferences）
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_background.xml      # 啟動圖標背景
│           │   ├── ic_launcher_foreground.xml      # 啟動圖標前景
│           │   ├── status_background_pending.xml   # 待處理狀態背景
│           │   ├── status_background_processing.xml # 處理中狀態背景
│           │   ├── status_background_success.xml   # 成功狀態背景
│           │   └── status_background_error.xml     # 錯誤狀態背景
│           ├── layout/
│           │   ├── activity_main.xml        # 主界面布局
│           │   └── item_subtitle_file.xml   # 文件列表項布局
│           ├── mipmap-*/                      # 啟動圖標（多種密度）
│           │   ├── ic_launcher.webp
│           │   └── ic_launcher_round.webp
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml          # 自適應圖標（API 26+）
│           │   └── ic_launcher_round.xml
│           └── values/
│               ├── colors.xml               # 顏色資源
│               ├── strings.xml              # 字串資源
│               └── themes.xml               # 主題樣式
├── build.gradle                   # 專案級 Gradle 配置
├── settings.gradle                # Gradle 設定（專案名稱與模組）
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar     # Gradle Wrapper JAR
│       └── gradle-wrapper.properties  # Gradle 版本配置
├── gradlew                        # Gradle Wrapper 腳本（Unix）
├── gradlew.bat                    # Gradle Wrapper 腳本（Windows）
├── gradle.properties              # Gradle 屬性配置
├── local.properties               # 本機 SDK 路徑（通常不提交到版本控制）
└── .gitignore                     # Git 忽略規則
```

## 技術規格

- **開發語言**: Kotlin 1.9.22
- **Compile SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **應用版本**: 1.0 (versionCode: 1)
- **Android Gradle Plugin (AGP)**: 8.2.0
- **Gradle Wrapper**: 9.0-milestone-1
- **Java 版本**: 1.8 (sourceCompatibility / targetCompatibility / jvmTarget)
- **構建特性**: ViewBinding 啟用
- **命名空間**: `com.example.lrcapp`

## 依賴項

### 主要依賴（Implementation）

- **AndroidX Core KTX** `1.12.0` - Kotlin 擴展庫
- **AndroidX AppCompat** `1.6.1` - 向後兼容支持庫
- **Material Design Components** `1.11.0` - Material Design 組件
- **ConstraintLayout** `2.1.4` - 約束布局
- **RecyclerView** `1.3.2` - 列表視圖組件
- **Lifecycle Runtime KTX** `2.7.0` - 生命週期管理（Kotlin 擴展）
- **Activity KTX** `1.8.2` - Activity Kotlin 擴展
- **Kotlin Coroutines Android** `1.7.3` - 協程支持

### 測試依賴（Test）

- **JUnit** `4.13.2` - 單元測試框架
- **AndroidX Test Ext JUnit** `1.1.5` - Android JUnit 擴展
- **Espresso Core** `3.5.1` - UI 自動化測試框架

### 注意事項

- **DocumentFile**：代碼中使用了 `androidx.documentfile.provider.DocumentFile`，但可能通過其他依賴傳遞引入，或需要手動添加 `androidx.documentfile:documentfile:1.0.1` 依賴

## 開發環境要求

### 必需環境

- **Android Studio**: Hedgehog (2023.1.1) 或更新版本
- **JDK**: 17 或更高版本（AGP 8.2.0 要求；Android Studio 內建的 JBR 17 也可使用）
- **Android SDK**: 
  - Platform 34 (Android 14)
  - Build Tools 34.0.0 或更新
  - Android SDK Command-line Tools

### 建議配置

- **記憶體**: 至少 8 GB RAM（建議 16 GB）
- **硬碟空間**: 至少 10 GB 可用空間（用於 SDK、模擬器等）
- **網路連線**: 首次構建需要下載依賴（約 500 MB - 1 GB）

### Gradle 配置

- **Gradle JVM 參數**: `-Xmx2048m -Dfile.encoding=UTF-8`（在 `gradle.properties` 中配置）
- **AndroidX**: 已啟用（`android.useAndroidX=true`）
- **非傳遞 R 類**: 已啟用（`android.nonTransitiveRClass=true`）

## 快速開始（建議）

1. 用 Android Studio 開啟專案根目錄（包含 `settings.gradle` 的資料夾）。
2. 等待 Gradle Sync 完成。
3. 建立/啟動模擬器或連接實體手機（需開啟 USB 偵錯）。
4. 點擊工具列的 Run（▶）運行 `app`。

## 使用流程

1. 啟動應用，進入主界面（標題顯示「LRC 字幕轉換器」）
2. （可能出現）依 Android 版本要求授予讀取/存取權限  
   - Android 11+：不需授予「所有檔案存取權」；選檔與選擇輸出目錄時會由系統文件選擇器提供 SAF 授權
   - Android 7~10：可能會跳出讀取外部儲存權限請求（`READ_EXTERNAL_STORAGE`）
3. （可選）設定輸出位置
   - 點擊「選擇目錄」設定自訂輸出資料夾（使用 SAF `OpenDocumentTree`）
   - 或開啟「輸出到原文件目錄」，改為逐來源資料夾授權後寫回原目錄
   - 兩者互斥，不能同時啟用
4. 點擊「選擇文件」，調出系統文件選擇器並一次選取多個字幕檔
5. 選取完成後，App 會先做前置校驗：不支援格式或超過 10MB 會直接標記「無效」狀態
   - **注意**：選擇新文件時會**清空舊列表**，替換為新選擇的文件
6. 點擊「開始轉換」，列表狀態會依序顯示「處理中 / 成功 / 錯誤」，並顯示進度百分比
7. 轉換完成後，**所有成功轉換的文件會自動保存**到指定的輸出目錄
   - 若開啟「輸出到原文件目錄」，第一次寫入某個來源資料夾時會要求授權該資料夾
   - 同一來源資料夾之後會重用已保存的授權
   - 顯示 Toast 提示：「轉換完成！成功: X / Y」
   - 顯示 Toast 提示：「已保存 X 個文件」

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
- 文件保存位置：預設寫入 App 外部檔案下載目錄；或由使用者在 App 內選擇自訂輸出資料夾；或在啟用「輸出到原文件目錄」後寫回已授權的來源資料夾

## 已知限制與設計決策

- **選擇器副檔名過濾**：目前未在系統選擇器中限制可見檔案，會在選取後才做副檔名驗證。
- **Android 11+ 權限策略**：目前檔案匯入、自訂輸出資料夾與原文件目錄輸出都依賴 SAF，不要求 `MANAGE_EXTERNAL_STORAGE`；預設輸出則使用 App-specific downloads。
- **字幕解析覆蓋範圍**：轉換器以常見格式規則實作，對於某些複雜情況（例如 VTT 多行 cue、ASS 逗號分隔更複雜的 Dialogue 欄位）可能需要進一步強化。
- **設置選項**：目前「智能命名清理」和「時間精度優化」在代碼中硬編碼為 `true`，沒有 UI 開關可調整（但轉換引擎仍會根據這些設置工作）；「輸出到原文件目錄」已有 UI 開關並會持久化。
- **文件導出方式**：轉換完成後會自動保存所有成功轉換的文件，不提供單個下載或 ZIP 打包選項（簡化用戶操作流程）。
- **文件列表管理**：選擇新文件時會清空舊列表，不支援追加文件到現有列表。





