package com.example.lrcapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lrcapp.adapter.SubtitleFileAdapter
import com.example.lrcapp.converter.SubtitleConverter
import com.example.lrcapp.model.AppSettings
import com.example.lrcapp.model.FileStatus
import com.example.lrcapp.model.SubtitleFile
import com.example.lrcapp.model.isEligibleForConversion
import com.example.lrcapp.util.FileNameHelper
import com.example.lrcapp.util.FileValidator
import com.example.lrcapp.util.SettingsManager
import com.example.lrcapp.util.StorageHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubtitleFileAdapter
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgress: TextView
    private lateinit var btnSelectFiles: MaterialButton
    private lateinit var btnConvert: MaterialButton
    private lateinit var tvOutputDir: TextView
    private lateinit var btnSelectOutputDir: Button
    private lateinit var toolbar: MaterialToolbar

    private val files = mutableListOf<SubtitleFile>()
    private var settings = AppSettings()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            openFilePicker()
        } else {
            Toast.makeText(this, "未授予讀取外部儲存的權限", Toast.LENGTH_SHORT).show()
        }
    }

    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            handleDirectorySelection(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSettings()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnSelectFiles = findViewById(R.id.btnSelectFiles)
        btnConvert = findViewById(R.id.btnConvert)
        tvOutputDir = findViewById(R.id.tvOutputDir)
        btnSelectOutputDir = findViewById(R.id.btnSelectOutputDir)
    }

    private fun loadSettings() {
        settings = SettingsManager.loadSettings(this)
        updateOutputDirDisplay()
    }

    private fun setupRecyclerView() {
        adapter = SubtitleFileAdapter(files)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSelectFiles.setOnClickListener {
            checkAndRequestImportPermission()
        }

        btnConvert.setOnClickListener {
            if (files.isEmpty()) {
                Toast.makeText(this, "請先選擇文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startConversion()
        }

        btnSelectOutputDir.setOnClickListener {
            directoryPickerLauncher.launch(null)
        }
    }

    private fun checkAndRequestImportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openFilePicker()
            return
        }

        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(permission))
        } else {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val mimeTypes = arrayOf("*/*")
        try {
            filePickerLauncher.launch(mimeTypes)
        } catch (e: Exception) {
            Toast.makeText(this, "無法打開文件選擇器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newFiles = mutableListOf<SubtitleFile>()

            for (uri in uris) {
                try {
                    val fileName = getFileName(uri)
                    val fileSize = getFileSize(uri)

                    val (isValid, errorMessage) = FileValidator.validateFile(fileName, fileSize)

                    val subtitleFile = SubtitleFile(
                        uri = uri,
                        fileName = fileName,
                        fileSize = fileSize,
                        status = if (isValid) FileStatus.PENDING else FileStatus.INVALID,
                        errorMessage = errorMessage
                    )

                    newFiles.add(subtitleFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                files.clear()
                files.addAll(newFiles)
                adapter.notifyDataSetChanged()
                Toast.makeText(
                    this@MainActivity,
                    "已選擇 ${newFiles.size} 個文件",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName.ifEmpty { "未知文件" }
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    private fun startConversion() {
        settings = SettingsManager.loadSettings(this)
        progressBar.progress = 0
        progressBar.visibility = android.view.View.VISIBLE
        tvProgress.visibility = android.view.View.VISIBLE
        val filesToProcess = files.filter { it.status.isEligibleForConversion() }
        if (filesToProcess.isEmpty()) {
            Toast.makeText(this, "沒有可轉換的文件", Toast.LENGTH_SHORT).show()
            progressBar.visibility = android.view.View.GONE
            tvProgress.visibility = android.view.View.GONE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val converter = SubtitleConverter(this@MainActivity, settings)
            var processedCount = 0

            for (file in filesToProcess) {
                val fileIndex = files.indexOf(file)
                if (fileIndex < 0) continue
                withContext(Dispatchers.Main) {
                    files[fileIndex].status = FileStatus.PROCESSING
                    adapter.updateFile(fileIndex, files[fileIndex])
                }
                try {
                    val lrcContent = converter.convertToLrc(file.uri, file.fileName)
                    if (lrcContent?.isNotEmpty() == true) {
                        val outputFileName = FileNameHelper.smartNaming(file.fileName, settings.smartNaming)
                        withContext(Dispatchers.Main) {
                            files[fileIndex].status = FileStatus.SUCCESS
                            files[fileIndex].outputFileName = outputFileName
                            files[fileIndex].lrcContent = lrcContent
                            files[fileIndex].errorMessage = null
                            adapter.updateFile(fileIndex, files[fileIndex])
                        }
                    } else {
                        throw Exception("解析錯誤或內容為空")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        files[fileIndex].status = FileStatus.ERROR
                        files[fileIndex].errorMessage = "轉換失敗: ${e.message}"
                        adapter.updateFile(fileIndex, files[fileIndex])
                    }
                }
                processedCount++
                val progress = (processedCount * 100) / filesToProcess.size
                withContext(Dispatchers.Main) {
                    progressBar.progress = progress
                    tvProgress.text = "進度: $progress%"
                }
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = android.view.View.GONE
                tvProgress.visibility = android.view.View.GONE
                val successCount = files.count { it.status == FileStatus.SUCCESS }
                if (successCount > 0) {
                    downloadAllFiles()
                }
                Toast.makeText(this@MainActivity, "轉換完成！成功: $successCount / ${filesToProcess.size}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadAllFiles() {
        val successFiles = files.filter { it.status == FileStatus.SUCCESS && it.lrcContent != null && it.outputFileName != null }
        if (successFiles.isEmpty()) {
            Toast.makeText(this, "沒有可導出的文件", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val filesToSave = successFiles.map { it.outputFileName!! to it.lrcContent!! }
            val outputDirUri = settings.outputDirUri?.let { Uri.parse(it) }
            val savedFiles = StorageHelper.saveMultipleFiles(this@MainActivity, outputDirUri, filesToSave)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "已保存 $savedFiles 個文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDirectorySelection(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        settings.outputDirUri = uri.toString()
        SettingsManager.saveSettings(this, settings)
        updateOutputDirDisplay()
    }

    private fun updateOutputDirDisplay() {
        val uriString = settings.outputDirUri
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val docFile = DocumentFile.fromTreeUri(this, uri)
            tvOutputDir.text = "儲存位置: ${docFile?.name}（SAF 授權目錄）"
        } else {
            tvOutputDir.text = "儲存位置: 預設應用下載目錄"
        }
    }
}
