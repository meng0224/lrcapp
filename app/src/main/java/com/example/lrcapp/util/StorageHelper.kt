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
import java.util.Date
import java.util.Locale
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

    private fun saveContentToUri(
        context: Context,
        dirUri: Uri,
        fileName: String,
        mimeType: String,
        contentBytes: ByteArray
    ): String? {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return null
            val targetFile = getOrCreateOutputDocument(dir, fileName, mimeType) ?: return null
            if (writeBytesToDocument(context, targetFile, contentBytes)) {
                targetFile.name
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    internal fun getOrCreateOutputDocument(dir: DocumentFile, fileName: String, mimeType: String): DocumentFile? {
        return dir.findFile(fileName) ?: dir.createFile(mimeType, fileName)
    }

    internal fun countSuccessfulResults(results: List<String?>): Int {
        return results.count { it != null }
    }

    private fun writeBytesToDocument(context: Context, file: DocumentFile, contentBytes: ByteArray): Boolean {
        return context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
            outputStream.write(contentBytes)
            outputStream.flush()
            true
        } ?: false
    }

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

    fun saveAsZip(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>, zipFileName: String): String? {
        if (outputDirUri != null) {
            return try {
                val dir = DocumentFile.fromTreeUri(context, outputDirUri) ?: return null
                val zipFile = getOrCreateOutputDocument(dir, zipFileName, "application/zip") ?: return null
                context.contentResolver.openOutputStream(zipFile.uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        for ((fileName, content) in files) {
                            zos.putNextEntry(ZipEntry(fileName))
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                    }
                } ?: return null
                zipFile.name
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

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

    fun saveMultipleFiles(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>): Int {
        if (outputDirUri != null) {
            val results = files.map { (fileName, content) ->
                saveContentToUri(context, outputDirUri, fileName, "application/octet-stream", content.toByteArray(Charsets.UTF_8))
            }
            return countSuccessfulResults(results)
        }

        var savedCount = 0
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
        return savedCount
    }
}
