# Android Studio 安裝與使用教學

本教學將指導您如何安裝 Android Studio 並使用它來開發和運行 LrcApp 專案。

## 目錄

1. [系統需求](#系統需求)
2. [下載 Android Studio](#下載-android-studio)
3. [安裝步驟](#安裝步驟)
4. [首次設置](#首次設置)
5. [打開專案](#打開專案)
6. [Gradle 同步](#gradle-同步)
7. [運行應用](#運行應用)
8. [基本操作](#基本操作)
9. [常見問題](#常見問題)

---

## 系統需求

### Windows 系統
- **作業系統**: Windows 10 (64-bit) 或更高版本
- **記憶體**: 至少 8 GB RAM（建議 16 GB）
- **硬碟空間**: 至少 8 GB 可用空間（建議 16 GB）
- **螢幕解析度**: 最低 1280 x 800

### macOS 系統
- **作業系統**: macOS 10.15 (Catalina) 或更高版本
- **記憶體**: 至少 8 GB RAM（建議 16 GB）
- **硬碟空間**: 至少 8 GB 可用空間（建議 16 GB）

### Linux 系統
- **作業系統**: 任何 64-bit Linux 發行版
- **記憶體**: 至少 8 GB RAM（建議 16 GB）
- **硬碟空間**: 至少 8 GB 可用空間（建議 16 GB）

---

## 下載 Android Studio

1. 訪問官方網站：https://developer.android.com/studio
2. 點擊「下載 Android Studio」按鈕
3. 閱讀並接受條款
4. 選擇適合您作業系統的版本下載
   - Windows: `.exe` 安裝檔
   - macOS: `.dmg` 映像檔
   - Linux: `.tar.gz` 壓縮檔

---

## 安裝步驟

### Windows 安裝

1. **執行安裝檔**
   - 雙擊下載的 `.exe` 檔案
   - 如果出現使用者帳戶控制提示，點擊「是」

2. **安裝精靈**
   - 點擊「Next」開始安裝
   - 選擇安裝元件（建議全部勾選）：
     - Android Studio
     - Android SDK
     - Android Virtual Device
     - Performance (Intel HAXM)
   - 選擇安裝路徑（預設即可）
   - 點擊「Next」繼續

3. **完成安裝**
   - 等待安裝完成
   - 勾選「Start Android Studio」後點擊「Finish」

### macOS 安裝

1. **掛載映像檔**
   - 雙擊下載的 `.dmg` 檔案
   - 將 Android Studio 拖曳到 Applications 資料夾

2. **首次啟動**
   - 打開 Applications 資料夾
   - 雙擊 Android Studio 圖示
   - 如果出現安全警告，前往「系統偏好設定」>「安全性與隱私權」允許執行

### Linux 安裝

1. **解壓縮**
   ```bash
   cd /opt
   sudo tar -xzf ~/Downloads/android-studio-*.tar.gz
   ```

2. **執行安裝腳本**
   ```bash
   cd android-studio/bin
   ./studio.sh
   ```

---

## 首次設置

### 1. 歡迎畫面設置

首次啟動 Android Studio 時，會出現設置精靈：

1. **選擇設置類型**
   - 選擇「Standard」（標準設置）或「Custom」（自訂設置）
   - 建議選擇「Standard」以使用預設配置

2. **選擇 UI 主題**
   - Light（淺色主題）
   - Darcula（深色主題）
   - 選擇您偏好的主題

3. **下載 SDK 元件**
   - Android Studio 會自動下載必要的 SDK 元件
   - 這可能需要一些時間，請耐心等待
   - 確保網路連線穩定

4. **驗證設置**
   - 檢查 SDK 路徑（通常為 `C:\Users\您的用戶名\AppData\Local\Android\Sdk`）
   - 確認已安裝的 SDK 版本

### 2. 安裝 Android SDK

1. 打開「SDK Manager」
   - 點擊工具列「More Actions」>「SDK Manager」
   - 或「File」>「Settings」>「Appearance & Behavior」>「System Settings」>「Android SDK」

2. **選擇 SDK 版本**
   - 勾選「Android 14.0 (API 34)」（LrcApp 專案所需）
   - 勾選「Android SDK Build-Tools」
   - 勾選「Android SDK Platform-Tools」
   - 勾選「Android Emulator」

3. **套用更改**
   - 點擊「Apply」開始下載
   - 等待下載完成

---

## 打開專案

### 方法一：從歡迎畫面打開

1. **啟動 Android Studio**
   - 如果已打開專案，點擊「File」>「Close Project」返回歡迎畫面

2. **打開專案**
   - 點擊「Open」或「Open an Existing Project」
   - 瀏覽到 LrcApp 專案資料夾（`D:\Git\lrcapp`）
   - 選擇專案根目錄（包含 `build.gradle` 和 `settings.gradle` 的資料夾）
   - 點擊「OK」

### 方法二：從檔案總管打開

1. **直接打開**
   - 在檔案總管中，找到 LrcApp 專案資料夾
   - 右鍵點擊專案資料夾
   - 選擇「Open with Android Studio」（如果已設定）

---

## Gradle 同步

打開專案後，Android Studio 會自動開始 Gradle 同步：

### 自動同步

1. **等待同步完成**
   - 查看底部狀態列，會顯示「Gradle sync in progress...」
   - 首次同步可能需要較長時間（5-10 分鐘）

2. **同步成功**
   - 狀態列顯示「Gradle sync completed」
   - 如果出現錯誤，請參考[常見問題](#常見問題)章節

### 手動同步

如果自動同步失敗，可以手動觸發：

1. **使用工具列**
   - 點擊工具列上的「Sync Project with Gradle Files」圖示（🔄）

2. **使用選單**
   - 點擊「File」>「Sync Project with Gradle Files」

3. **使用快捷鍵**
   - Windows/Linux: `Ctrl + Shift + O`
   - macOS: `Cmd + Shift + O`

---

## 運行應用

### 準備設備

您需要一個 Android 設備或模擬器來運行應用：

#### 選項一：使用實體設備

1. **啟用開發者選項**
   - 前往「設定」>「關於手機」
   - 連續點擊「版本號碼」7 次
   - 返回「設定」，找到「開發者選項」

2. **啟用 USB 偵錯**
   - 進入「開發者選項」
   - 開啟「USB 偵錯」
   - 使用 USB 線連接手機到電腦

3. **授權電腦**
   - 手機上會出現「允許 USB 偵錯？」提示
   - 勾選「一律允許這部電腦」後點擊「確定」

4. **驗證連接**
   - 在 Android Studio 中，點擊工具列的設備選擇器
   - 應該能看到您的設備名稱

#### 選項二：使用 Android 模擬器

1. **創建模擬器**
   - 點擊工具列「More Actions」>「Virtual Device Manager」
   - 或「Tools」>「Device Manager」

2. **創建設備**
   - 點擊「Create Device」
   - 選擇設備類型（建議選擇「Pixel 5」或「Pixel 6」）
   - 點擊「Next」

3. **選擇系統映像**
   - 選擇「API 34」（Android 14）
   - 如果未下載，點擊「Download」下載
   - 點擊「Next」

4. **完成設置**
   - 確認設備配置
   - 點擊「Finish」

5. **啟動模擬器**
   - 在 Device Manager 中，點擊設備旁的「Play」按鈕
   - 等待模擬器啟動（首次啟動可能需要幾分鐘）

### 運行應用

1. **選擇運行配置**
   - 確認工具列的設備選擇器顯示正確的設備
   - 確認運行配置為「app」

2. **運行應用**
   - 點擊工具列的「Run」按鈕（綠色播放圖示 ▶）
   - 或使用快捷鍵：
     - Windows/Linux: `Shift + F10`
     - macOS: `Ctrl + R`

3. **等待構建**
   - Android Studio 會先編譯專案
   - 構建完成後，應用會自動安裝到設備並啟動

4. **查看日誌**
   - 底部「Logcat」視窗會顯示應用日誌
   - 如果有錯誤，會以紅色顯示

---

## 基本操作

### 專案結構

在左側「Project」視窗中，您可以查看專案結構：

```
app/
├── manifests/          # AndroidManifest.xml
├── java/              # Kotlin/Java 源代碼
│   └── com/example/lrcapp/
├── res/               # 資源文件
│   ├── layout/        # 布局文件
│   ├── values/        # 字串、顏色等
│   └── ...
└── build.gradle       # 模組級構建配置
```

### 編輯代碼

1. **打開文件**
   - 在 Project 視窗中雙擊文件即可打開
   - 使用 `Ctrl + P`（macOS: `Cmd + P`）快速搜尋文件

2. **代碼補全**
   - 輸入時會自動顯示建議
   - 使用 `Ctrl + Space` 手動觸發補全

3. **格式化代碼**
   - 右鍵點擊 >「Reformat Code」
   - 或使用快捷鍵：`Ctrl + Alt + L`（macOS: `Cmd + Option + L`）

### 調試應用

1. **設置斷點**
   - 在代碼行號左側點擊，設置紅色斷點

2. **調試模式運行**
   - 點擊工具列的「Debug」按鈕（蟲子圖示 🐛）
   - 或使用快捷鍵：`Shift + F9`（macOS: `Ctrl + D`）

3. **查看變數**
   - 在「Debug」視窗中查看變數值
   - 滑鼠懸停在變數上可查看值

### 構建 APK

1. **生成簽名 APK**
   - 點擊「Build」>「Build Bundle(s) / APK(s)」>「Build APK(s)」

2. **查看輸出**
   - 構建完成後，點擊通知中的「locate」
   - APK 文件位於 `app/build/outputs/apk/debug/app-debug.apk`

---

## 常見問題

### 1. Gradle 同步失敗

**問題**: Gradle sync 失敗，出現錯誤訊息

**解決方案**:
- 檢查網路連線（需要下載依賴）
- 檢查 Gradle 版本是否正確
- 嘗試「File」>「Invalidate Caches / Restart」>「Invalidate and Restart」
- 刪除 `.gradle` 資料夾後重新同步

### 2. SDK 未找到

**問題**: 提示找不到 Android SDK

**解決方案**:
1. 打開「File」>「Settings」>「Appearance & Behavior」>「System Settings」>「Android SDK」
2. 檢查「Android SDK Location」路徑是否正確
3. 如果路徑錯誤，點擊「Edit」修正

### 3. 模擬器啟動緩慢

**問題**: Android 模擬器啟動很慢

**解決方案**:
- 確保已啟用硬體加速（HAXM 或 HAXM 替代方案）
- 增加模擬器的 RAM 分配
- 使用較新的模擬器版本

### 4. 設備未識別

**問題**: 實體設備連接後無法識別

**解決方案**:
- 確認已啟用 USB 偵錯
- 檢查 USB 驅動程式是否已安裝
- 嘗試更換 USB 線或 USB 埠
- 在命令列執行 `adb devices` 檢查設備是否出現

### 5. 構建錯誤

**問題**: 構建時出現錯誤

**解決方案**:
- 查看「Build」視窗中的錯誤訊息
- 確認所有依賴都已正確下載
- 嘗試「Build」>「Clean Project」，然後「Build」>「Rebuild Project」

### 6. 記憶體不足

**問題**: Android Studio 運行緩慢或崩潰

**解決方案**:
1. 增加 Android Studio 記憶體分配：
   - 「Help」>「Edit Custom VM Options」
   - 修改 `-Xmx` 參數（例如：`-Xmx4096m` 表示 4GB）
2. 關閉不必要的視窗和工具
3. 增加系統 RAM（如果可能）

### 7. Kotlin 版本不匹配

**問題**: Kotlin 版本相關錯誤

**解決方案**:
- 檢查 `build.gradle` 中的 Kotlin 版本
- 確保專案級和模組級的 Kotlin 版本一致
- 更新到最新穩定版本

---

## 快捷鍵參考

### Windows/Linux

| 功能 | 快捷鍵 |
|------|--------|
| 運行應用 | `Shift + F10` |
| 調試應用 | `Shift + F9` |
| 停止運行 | `Ctrl + F2` |
| 同步 Gradle | `Ctrl + Shift + O` |
| 搜尋文件 | `Ctrl + Shift + N` |
| 搜尋符號 | `Ctrl + Alt + Shift + N` |
| 格式化代碼 | `Ctrl + Alt + L` |
| 快速修復 | `Alt + Enter` |
| 顯示使用處 | `Alt + F7` |

### macOS

| 功能 | 快捷鍵 |
|------|--------|
| 運行應用 | `Ctrl + R` |
| 調試應用 | `Ctrl + D` |
| 停止運行 | `Cmd + F2` |
| 同步 Gradle | `Cmd + Shift + O` |
| 搜尋文件 | `Cmd + Shift + O` |
| 搜尋符號 | `Cmd + Option + O` |
| 格式化代碼 | `Cmd + Option + L` |
| 快速修復 | `Option + Enter` |
| 顯示使用處 | `Option + F7` |

---

## 進階技巧

### 1. 使用版本控制

Android Studio 內建 Git 支援：

1. **啟用版本控制**
   - 「VCS」>「Enable Version Control Integration」
   - 選擇「Git」

2. **提交更改**
   - 「VCS」>「Commit」
   - 或使用快捷鍵：`Ctrl + K`（macOS: `Cmd + K`）

### 2. 使用 Live Templates

快速生成常用代碼片段：

- 輸入 `logd` 然後按 `Tab` 生成 Log.d
- 輸入 `Toast` 然後按 `Tab` 生成 Toast 代碼

### 3. 使用插件

擴展 Android Studio 功能：

1. 「File」>「Settings」>「Plugins」
2. 搜尋並安裝需要的插件
3. 推薦插件：
   - ADB Idea（ADB 工具增強）
   - Rainbow Brackets（彩色括號）
   - Material Theme UI（主題）

---

## 獲取幫助

### 官方資源

- **官方文檔**: https://developer.android.com/studio
- **開發者指南**: https://developer.android.com/guide
- **API 參考**: https://developer.android.com/reference

### 社群支援

- **Stack Overflow**: https://stackoverflow.com/questions/tagged/android-studio
- **Reddit**: r/androiddev
- **官方論壇**: https://developer.android.com/community

---

## 下一步

現在您已經熟悉 Android Studio 的基本使用，可以開始：

1. ✅ 打開 LrcApp 專案
2. ✅ 同步 Gradle
3. ✅ 運行應用
4. 📝 開始開發和修改代碼
5. 🐛 使用調試工具解決問題
6. 📦 構建並分享您的應用

祝您開發愉快！🎉
