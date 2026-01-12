package com.example.lrcapp.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StorageHelper {

    fun getDownloadDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    private fun saveContentToUri(context: Context, dirUri: Uri, fileName: String, mimeType: String, contentBytes: ByteArray): String? {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return null
            val existingFile = dir.findFile(fileName)
            if (existingFile != null) {
                existingFile.delete()
            }
            val newFile = dir.createFile(mimeType, fileName) ?: return null
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(contentBytes)
            }
            newFile.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存 LRC 文件
     * @return Saved file name, or null on failure
     */
    fun saveLrcFile(context: Context, outputDirUri: Uri?, fileName: String, content: String): String? {
        return if (outputDirUri != null) {
            saveContentToUri(context, outputDirUri, fileName, "application/octet-stream", content.toByteArray(Charsets.UTF_8))
        } else {
            try {
                val downloadDir = getDownloadDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                file.name
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun generateZipFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "字幕轉換_$timestamp.zip"
    }

    /**
     * 保存多個文件為 ZIP
     * @return Saved ZIP file name, or null on failure
     */
    fun saveAsZip(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>, zipFileName: String): String? {
        if (outputDirUri != null) {
            return try {
                val dir = DocumentFile.fromTreeUri(context, outputDirUri) ?: return null
                val existingFile = dir.findFile(zipFileName)
                if (existingFile != null) {
                    existingFile.delete()
                }
                val zipFile = dir.createFile("application/zip", zipFileName) ?: return null
                context.contentResolver.openOutputStream(zipFile.uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        for ((fileName, content) in files) {
                            zos.putNextEntry(ZipEntry(fileName))
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                    }
                }
                zipFile.name
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            return try {
                val downloadDir = getDownloadDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val zipFile = File(downloadDir, zipFileName)
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    for ((fileName, content) in files) {
                        zos.putNextEntry(ZipEntry(fileName))
                        zos.write(content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                zipFile.name
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 批量保存文件（不壓縮）
     * @return Number of successfully saved files
     */
    fun saveMultipleFiles(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>): Int {
        var savedCount = 0
        if (outputDirUri != null) {
            files.forEach { (fileName, content) ->
                val result = saveContentToUri(context, outputDirUri, fileName, "application/octet-stream", content.toByteArray(Charsets.UTF_8))
                if (result != null) {
                    savedCount++
                }
            }
        } else {
            val downloadDir = getDownloadDirectory(context)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            for ((fileName, content) in files) {
                try {
                    val file = File(downloadDir, fileName)
                    FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    savedCount++
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return savedCount
    }
}
