> generated_by: nexus-mapper v2
> verified_at: 2026-03-18
> provenance: AST-backed for Kotlin module/class inventory; internal dependency boundaries below are partly inferred from manual import reading because query_graph treated same-package Kotlin imports as external in this environment.

# LrcApp Nexus Map

LrcApp 是單一 `app/` module 的 Android 字幕轉換工具，核心流程集中在 [MainActivity.kt](D:/06_開發與工具/Git/lrcapp/app/src/main/java/com/example/lrcapp/MainActivity.kt)。它負責選檔、遞迴匯入、SAF 授權、批次轉換、輸出保存與 UI 狀態。

主要系統共有三塊：
- UI Orchestrator：`MainActivity.kt` 與 RecyclerView/UI layout，負責工作流與所有使用者可見狀態。
- Conversion Engine：`converter/SubtitleConverter.kt`，將 `VTT/ASS/SSA/SRT/STR/SMI/SUB` 轉成 LRC。
- Storage And Policy Services：`util/`，封裝檔案驗證、命名、列表合併、輸出模式互斥、設定持久化與實際寫入。

關鍵風險集中在 UI 編排層：
- `MainActivity.kt` 90 天內 15 次變更，且與 `activity_main.xml`、`StorageHelper.kt`、`SettingsManager.kt` 高度共變，表示需求演進主要壓在單一 Activity。
- `StorageHelper.kt` 是第二個實際核心，承接 SAF 目錄寫入、檔名校驗與相對路徑重建；其測試相對完整。
- AST 對 Kotlin 結構覆蓋完整，但這次 `query_graph` 未把 `com.example.lrcapp.*` 視為內部 import，所以系統依賴關係需搭配人工閱讀看待。

測試面以 JVM unit tests 為主，涵蓋轉換器、列表策略、檔名/格式檢查、設定鍵與 StorageHelper 純邏輯；UI 啟動狀態則有 1 個 instrumentation test 類別。evidence gap: 本次沒有執行 Gradle 測試，也沒有驗證模擬器/裝置可用性。

unknown: 目前沒有獨立 service、repository 或 use-case 層；若未來持續擴張 SAF/授權流程，`MainActivity.kt` 會成為最先需要拆分的邊界。

## [操作指南] 強制执行步骤

> 本节是对所有读取本文件的 AI 发出的硬性操作指令，不是建议。

### 步骤1 — 必须先读完以下所有文件（顺序不限）

读完本文件后，在执行任何任务之前，必须依次 read 以下文件完整内容：

- `.nexus-map/arch/systems.md` — 系统边界与代码位置
- `.nexus-map/arch/dependencies.md` — 系统间依赖关系与 Mermaid 图
- `.nexus-map/arch/test_coverage.md` — 测试面与证据缺口
- `.nexus-map/hotspots/git_forensics.md` — Git 热点与耦合风险
- `.nexus-map/concepts/domains.md` — 核心领域概念

> 这些文件均为高密度摘要，总量通常 < 5000 tokens，是必要的上下文成本。
> 不得以"任务简单"或"只改一个文件"为由跳过。

### 步骤2 — 按任务类型追加操作（步骤1 完成后执行）

- 若任务涉及**接口修改、新增跨模块调用、删除/重命名公共函数**：
  → 必须运行 `query_graph.py --impact <目标文件>` 确认影响半径后再写代码。
- 若任务需要**判断某文件被谁引用**：
  → 运行 `query_graph.py --who-imports <模块名>`。
- 若仓库结构已发生重大变化（新增系统、重构模块边界）：
  → 任务完成后评估是否需要重新运行 nexus-mapper 更新知识库。
