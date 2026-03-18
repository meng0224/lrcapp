> generated_by: nexus-mapper v2
> verified_at: 2026-03-18
> provenance: Derived from README, Kotlin source, and test names; no unsupported language downgrade was needed.

# Core Domains

## Subtitle Intake

使用者可以一次選多個檔案，或在遞迴匯入模式下選擇根目錄並深度掃描子資料夾。一般模式會保留無效檔並標示錯誤；遞迴匯入則直接略過無效檔。URI 是列表去重的主鍵，不是檔名。

## Conversion Semantics

轉換器以副檔名決定解析器，輸出固定為 LRC。`smartNaming` 與 `timePrecision` 在目前產品中固定啟用，雖然設定模型仍保留欄位，但 UI 不提供切換。

## Output Targeting

輸出模式有三種：預設 app-specific downloads、自訂 SAF 目錄、原文件目錄。原文件目錄與自訂目錄互斥；遞迴匯入寫回原目錄時，需要先授權匯入根目錄並在輸出時重建相對子資料夾。

## Authorization Memory

來源目錄與匯入根目錄的授權 URI 以編碼後的 key 存入 SharedPreferences。這讓保存流程可以在之後重用權限，而不是每次都重新授權。

## File State Lifecycle

`SubtitleFile` 在列表中流經 `PENDING -> PROCESSING -> SUCCESS/ERROR`，無效檔則直接是 `INVALID`。`ERROR` 狀態仍可再次轉換，這也是 `isEligibleForConversion()` 的設計重點。
