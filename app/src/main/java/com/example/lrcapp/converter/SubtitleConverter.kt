package com.example.lrcapp.converter

import android.content.Context
import android.net.Uri
import com.example.lrcapp.model.AppSettings
import com.example.lrcapp.util.FileValidator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * 字幕轉換器核心類
 */
class SubtitleConverter(private val context: Context, private val settings: AppSettings) {

    /**
     * 轉換字幕文件為 LRC 格式
     */
    fun convertToLrc(uri: Uri, fileName: String): String? {
        return try {
            val content = readFileContent(uri)
            val extension = FileValidator.getExtension(fileName)
            
            when (extension) {
                "vtt" -> convertVttToLrc(content)
                "srt" -> convertSrtToLrc(content)
                "ass", "ssa" -> convertAssToLrc(content)
                "smi" -> convertSmiToLrc(content)
                "sub", "str" -> convertSubToLrc(content)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 讀取文件內容（UTF-8）
     */
    private fun readFileContent(uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                return reader.readText()
            }
        } ?: throw Exception("無法讀取文件")
    }

    /**
     * 轉換 VTT 格式
     */
    private fun convertVttToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        
        // VTT 時間格式: 00:00:00.000 --> 00:00:01.000
        val timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})")
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.contains("-->")) {
                val timeMatch = timePattern.matcher(line)
                if (timeMatch.find()) {
                    val startTime = formatTimeToLrc(
                        timeMatch.group(1).toInt(), // 小時
                        timeMatch.group(2).toInt(), // 分鐘
                        timeMatch.group(3).toInt(), // 秒
                        timeMatch.group(4).toInt()  // 毫秒
                    )
                    
                    // 讀取後續文本行，直到遇到空行
                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }
                    
                    val text = cleanText(textLines.joinToString(" "))
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$startTime]$text")
                    }
                    continue // 繼續外層循環
                }
            }
            i++
        }
        
        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SRT 格式
     */
    private fun convertSrtToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        
        // SRT 時間格式: 00:00:00,000 --> 00:00:01,000 或 00:00:00.000 --> 00:00:01.000
        val timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{3})")
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // 只尋找時間軸
            if (line.contains("-->")) {
                val timeMatch = timePattern.matcher(line)
                if (timeMatch.find()) {
                    val startTime = formatTimeToLrc(
                        timeMatch.group(1).toInt(),
                        timeMatch.group(2).toInt(),
                        timeMatch.group(3).toInt(),
                        timeMatch.group(4).toInt()
                    )
                    
                    // 讀取後續文本行，直到遇到空行
                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }
                    
                    val text = cleanText(textLines.joinToString(" "))
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$startTime]$text")
                    }
                    continue // 繼續外層循環
                }
            }
            i++
        }
        
        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 ASS/SSA 格式
     */
    private fun convertAssToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        
        // ASS/SSA Dialogue 格式: Dialogue: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,文本內容
        val dialoguePattern = Pattern.compile("Dialogue:\\s*[^,]*,\\s*(\\d+):(\\d{2}):(\\d{2})[.,](\\d{2})")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // 跳過註釋和樣式定義
            if (trimmedLine.startsWith(";") || 
                trimmedLine.startsWith("[Script Info]") ||
                trimmedLine.startsWith("[V4+ Styles]") ||
                trimmedLine.startsWith("[Events]") ||
                trimmedLine.startsWith("Format:")) {
                continue
            }
            
            // 處理 Dialogue 行
            if (trimmedLine.startsWith("Dialogue:")) {
                val match = dialoguePattern.matcher(trimmedLine)
                if (match.find()) {
                    val startTime = formatTimeToLrc(
                        match.group(1).toInt(),
                        match.group(2).toInt(),
                        match.group(3).toInt(),
                        match.group(4).toInt() * 10 // 百分秒轉毫秒
                    )
                    
                    // 提取文本內容（最後一個逗號後的部分）
                    val textStart = trimmedLine.lastIndexOf(',') + 1
                    if (textStart > 0 && textStart < trimmedLine.length) {
                        var text = trimmedLine.substring(textStart)
                        text = cleanAssText(text)
                        if (text.isNotEmpty()) {
                            lrcLines.add("[$startTime]$text")
                        }
                    }
                }
            }
        }
        
        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SMI 格式
     */
    private fun convertSmiToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        
        // SMI 時間格式: <SYNC Start=12345>
        val syncPattern = Pattern.compile("<SYNC\\s+Start=(\\d+)>", Pattern.CASE_INSENSITIVE)
        
        var currentTime = 0L
        var currentText = StringBuilder()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // 跳過 HTML 標籤和註釋
            if (trimmedLine.startsWith("<!--") || trimmedLine.startsWith("<HEAD>") || 
                trimmedLine.startsWith("</HEAD>") || trimmedLine.startsWith("<BODY>") ||
                trimmedLine.startsWith("</BODY>")) {
                continue
            }
            
            // 處理 SYNC 標籤
            val syncMatch = syncPattern.matcher(trimmedLine)
            if (syncMatch.find()) {
                // 保存前一個時間點的文本
                if (currentTime > 0 && currentText.isNotEmpty()) {
                    val timeStr = formatTimeFromMilliseconds(currentTime)
                    val text = cleanText(currentText.toString())
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$timeStr]$text")
                    }
                }
                
                currentTime = syncMatch.group(1).toLong()
                currentText.clear()
            } else if (trimmedLine.startsWith("<P") || trimmedLine.startsWith("</P>")) {
                // 段落標籤，忽略
            } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("<")) {
                // 文本內容
                if (currentText.isNotEmpty()) {
                    currentText.append(" ")
                }
                currentText.append(cleanText(trimmedLine))
            }
        }
        
        // 處理最後一個時間點
        if (currentTime > 0 && currentText.isNotEmpty()) {
            val timeStr = formatTimeFromMilliseconds(currentTime)
            val text = cleanText(currentText.toString())
            if (text.isNotEmpty()) {
                lrcLines.add("[$timeStr]$text")
            }
        }
        
        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SUB 格式
     */
    private fun convertSubToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        
        // SUB 時間格式: {12345}{12350}文本內容
        val subPattern = Pattern.compile("\\{(\\d+)\\}\\{(\\d+)\\}(.*)")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            val match = subPattern.matcher(trimmedLine)
            if (match.find()) {
                val startTimeMs = match.group(1).toLong()
                val text = cleanText(match.group(3))
                
                if (text.isNotEmpty()) {
                    val timeStr = formatTimeFromMilliseconds(startTimeMs)
                    lrcLines.add("[$timeStr]$text")
                }
            }
        }
        
        return lrcLines.joinToString("\n")
    }

    /**
     * 清理文本（移除 HTML 標籤、樣式代碼等）
     */
    private fun cleanText(text: String): String {
        var cleaned = text
            // 移除 HTML 標籤
            .replace(Regex("<[^>]+>"), "")
            // 移除多餘空格
            .replace(Regex("\\s+"), " ")
            .trim()
        
        return cleaned
    }

    /**
     * 清理 ASS 文本（移除樣式代碼）
     */
    private fun cleanAssText(text: String): String {
        var cleaned = text
            // 移除 ASS 樣式代碼 {\\...}
            .replace(Regex("\\{[^}]*\\}"), "")
            // 移除 HTML 標籤
            .replace(Regex("<[^>]+>"), "")
            // 移除多餘空格
            .replace(Regex("\\s+"), " ")
            .trim()
        
        return cleaned
    }

    /**
     * 格式化時間為 LRC 格式 [mm:ss.xx]
     */
    private fun formatTimeToLrc(hours: Int, minutes: Int, seconds: Int, milliseconds: Int): String {
        val totalMinutes = hours * 60 + minutes
        val totalSeconds = seconds
        
        return if (settings.timePrecision) {
            // 保留毫秒精度
            val centiseconds = milliseconds / 10
            String.format("%02d:%02d.%02d", totalMinutes, totalSeconds, centiseconds)
        } else {
            // 只保留秒
            String.format("%02d:%02d.00", totalMinutes, totalSeconds)
        }
    }

    /**
     * 從毫秒數格式化時間
     */
    private fun formatTimeFromMilliseconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val centiseconds = ((milliseconds % 1000) / 10).toInt()
        
        return if (settings.timePrecision) {
            String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
        } else {
            String.format("%02d:%02d.00", minutes, seconds)
        }
    }
}
