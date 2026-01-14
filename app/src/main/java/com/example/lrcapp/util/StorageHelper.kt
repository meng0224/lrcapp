package com.example.lrcapp.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StorageHelper {

    private fun getDownloadDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    private fun saveToFile(context: Context, outputDirUri: Uri?, fileName: String, mimeType: String, writer: (OutputStream) -> Unit): String? {
        return try {
            if (outputDirUri != null) {
                val dir = DocumentFile.fromTreeUri(context, outputDirUri) ?: return null
                val existingFile = dir.findFile(fileName)
                if (existingFile != null) {
                    existingFile.delete()
                }
                val newFile = dir.createFile(mimeType, fileName) ?: return null
                context.contentResolver.openOutputStream(newFile.uri)?.use(writer)
                newFile.name
            } else {
                val downloadDir = getDownloadDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use(writer)
                file.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveLrcFile(context: Context, outputDirUri: Uri?, fileName: String, content: String): String? {
        return saveToFile(context, outputDirUri, fileName, "application/octet-stream") { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun generateZipFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "字幕轉換_$timestamp.zip"
    }

    fun saveAsZip(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>, zipFileName: String): String? {
        return saveToFile(context, outputDirUri, zipFileName, "application/zip") { outputStream ->
            ZipOutputStream(outputStream).use { zos ->
                for ((fileName, content) in files) {
                    zos.putNextEntry(ZipEntry(fileName))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
        }
    }

    fun saveMultipleFiles(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>): Int {
        var savedCount = 0
        files.forEach { (fileName, content) ->
            val result = saveLrcFile(context, outputDirUri, fileName, content)
            if (result != null) {
                savedCount++
            }
        }
        return savedCount
    }
}
