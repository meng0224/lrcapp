package com.example.lrcapp.util

object FileListUiPolicy {
    fun canClearFileList(fileCount: Int): Boolean {
        return fileCount > 0
    }
}
