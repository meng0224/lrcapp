package com.example.lrcapp.model

import android.net.Uri
import java.io.File

data class SubtitleFile(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    var status: FileStatus = FileStatus.PENDING,
    var errorMessage: String? = null,
    var outputFileName: String? = null,
    var lrcContent: String? = null
)

enum class FileStatus {
    PENDING,      // 待處理
    PROCESSING,   // 處理中
    SUCCESS,      // 成功
    ERROR         // 錯誤
}
