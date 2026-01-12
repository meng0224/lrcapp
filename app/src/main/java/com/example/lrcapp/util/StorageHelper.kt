package com.example.lrcapp.util

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object StorageHelper {
    /**
     * 獲取下載目錄
     */
    fun getDownloadDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 Scoped Storage
            ContextCompat.getExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS)
                ?: context.getExternalFilesDir(null) ?: context.filesDir
        } else {
            // Android 10 以下使用傳統存儲
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    /**
     * 保存 LRC 文件
     */
    fun saveLrcFile(context: Context, fileName: String, content: String): File? {
        return try {
            val downloadDir = getDownloadDirectory(context)
            val file = File(downloadDir, fileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 創建 ZIP 文件名（帶時間戳）
     */
    fun generateZipFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "字幕轉換_$timestamp.zip"
    }

    /**
     * 保存多個文件為 ZIP
     */
    fun saveAsZip(context: Context, files: List<Pair<String, String>>, zipFileName: String): File? {
        return try {
            val downloadDir = getDownloadDirectory(context)
            val zipFile = File(downloadDir, zipFileName)
            
            java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for ((fileName, content) in files) {
                    val entry = java.util.zip.ZipEntry(fileName)
                    zos.putNextEntry(entry)
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            
            zipFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 批量保存文件（不壓縮）
     */
    fun saveMultipleFiles(context: Context, files: List<Pair<String, String>>): List<File> {
        val savedFiles = mutableListOf<File>()
        val downloadDir = getDownloadDirectory(context)
        
        for ((fileName, content) in files) {
            try {
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(content.toByteArray(Charsets.UTF_8))
                }
                savedFiles.add(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        
        return savedFiles
    }
}
