package com.example.lrcapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lrcapp.adapter.SubtitleFileAdapter
import com.example.lrcapp.converter.SubtitleConverter
import com.example.lrcapp.model.AppSettings
import com.example.lrcapp.model.FileStatus
import com.example.lrcapp.model.SubtitleFile
import com.example.lrcapp.util.FileNameHelper
import com.example.lrcapp.util.FileValidator
import com.example.lrcapp.util.SettingsManager
import com.example.lrcapp.util.StorageHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubtitleFileAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnSelectFiles: MaterialButton
    private lateinit var btnConvert: MaterialButton
    private lateinit var btnDownloadZip: MaterialButton
    private lateinit var btnDownloadAll: MaterialButton
    private lateinit var switchSmartNaming: SwitchMaterial
    private lateinit var switchTimePrecision: SwitchMaterial
    private lateinit var exportButtonsLayout: android.widget.LinearLayout

    private val files = mutableListOf<SubtitleFile>()
    private var settings = AppSettings()

    // 文件選擇器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

    // 權限請求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, "需要存儲權限才能選擇文件", Toast.LENGTH_SHORT).show()
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
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnSelectFiles = findViewById(R.id.btnSelectFiles)
        btnConvert = findViewById(R.id.btnConvert)
        btnDownloadZip = findViewById(R.id.btnDownloadZip)
        btnDownloadAll = findViewById(R.id.btnDownloadAll)
        switchSmartNaming = findViewById(R.id.switchSmartNaming)
        switchTimePrecision = findViewById(R.id.switchTimePrecision)
        exportButtonsLayout = findViewById(R.id.exportButtonsLayout)
    }

    private fun loadSettings() {
        settings = SettingsManager.loadSettings(this)
        switchSmartNaming.isChecked = settings.smartNaming
        switchTimePrecision.isChecked = settings.timePrecision

        switchSmartNaming.setOnCheckedChangeListener { _, isChecked ->
            settings.smartNaming = isChecked
            SettingsManager.saveSettings(this, settings)
        }

        switchTimePrecision.setOnCheckedChangeListener { _, isChecked ->
            settings.timePrecision = isChecked
            SettingsManager.saveSettings(this, settings)
        }
    }

    private fun setupRecyclerView() {
        adapter = SubtitleFileAdapter(files) { file ->
            downloadSingleFile(file)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSelectFiles.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }

        btnConvert.setOnClickListener {
            if (files.isEmpty()) {
                Toast.makeText(this, "請先選擇文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startConversion()
        }

        btnDownloadZip.setOnClickListener {
            downloadAsZip()
        }

        btnDownloadAll.setOnClickListener {
            downloadAllFiles()
        }
    }

    private fun checkPermissionsAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 READ_MEDIA_FILES
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_FILES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openFilePicker()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_FILES))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要權限，直接打開
            openFilePicker()
        } else {
            // Android 10 以下需要 READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openFilePicker()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private fun openFilePicker() {
        val mimeTypes = arrayOf(
            "text/vtt",
            "text/x-subrip",
            "text/x-ssa",
            "text/x-ass",
            "application/x-subrip",
            "application/x-subtitle",
            "*/*"
        )
        filePickerLauncher.launch(mimeTypes)
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newFiles = mutableListOf<SubtitleFile>()

            for (uri in uris) {
                try {
                    val fileName = getFileName(uri)
                    val fileSize = getFileSize(uri)

                    // 前置校驗
                    val (isValid, errorMessage) = FileValidator.validateFile(fileName, fileSize)

                    val subtitleFile = SubtitleFile(
                        uri = uri,
                        fileName = fileName,
                        fileSize = fileSize,
                        status = if (isValid) FileStatus.PENDING else FileStatus.ERROR,
                        errorMessage = errorMessage
                    )

                    newFiles.add(subtitleFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
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
        // 更新設置
        settings = SettingsManager.loadSettings(this)

        // 重置進度
        progressBar.progress = 0
        progressBar.visibility = android.view.View.VISIBLE
        tvProgress.visibility = android.view.View.VISIBLE
        exportButtonsLayout.visibility = android.view.View.GONE

        // 只處理待處理和錯誤狀態的文件（重新處理錯誤文件）
        val filesToProcess = files.filter { it.status == FileStatus.PENDING || it.status == FileStatus.ERROR }

        if (filesToProcess.isEmpty()) {
            Toast.makeText(this, "沒有需要轉換的文件", Toast.LENGTH_SHORT).show()
            progressBar.visibility = android.view.View.GONE
            tvProgress.visibility = android.view.View.GONE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val converter = SubtitleConverter(this@MainActivity, settings)
            var processedCount = 0

            for ((index, file) in filesToProcess.withIndex()) {
                val fileIndex = files.indexOf(file)
                if (fileIndex < 0) continue

                // 更新狀態為處理中
                withContext(Dispatchers.Main) {
                    files[fileIndex].status = FileStatus.PROCESSING
                    adapter.updateFile(fileIndex, files[fileIndex])
                }

                try {
                    // 轉換文件
                    val lrcContent = converter.convertToLrc(file.uri, file.fileName)

                    if (lrcContent != null && lrcContent.isNotEmpty()) {
                        // 生成輸出文件名
                        val outputFileName = FileNameHelper.smartNaming(
                            file.fileName,
                            settings.smartNaming
                        )

                        withContext(Dispatchers.Main) {
                            files[fileIndex].status = FileStatus.SUCCESS
                            files[fileIndex].outputFileName = outputFileName
                            files[fileIndex].lrcContent = lrcContent
                            files[fileIndex].errorMessage = null
                            adapter.updateFile(fileIndex, files[fileIndex])
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            files[fileIndex].status = FileStatus.ERROR
                            files[fileIndex].errorMessage = "解析錯誤"
                            adapter.updateFile(fileIndex, files[fileIndex])
                        }
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
                    exportButtonsLayout.visibility = android.view.View.VISIBLE
                }

                Toast.makeText(
                    this@MainActivity,
                    "轉換完成！成功: $successCount / ${filesToProcess.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadSingleFile(file: SubtitleFile) {
        if (file.lrcContent == null || file.outputFileName == null) {
            Toast.makeText(this, "文件內容為空", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val savedFile = StorageHelper.saveLrcFile(
                this@MainActivity,
                file.outputFileName!!,
                file.lrcContent!!
            )

            withContext(Dispatchers.Main) {
                if (savedFile != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "已保存: ${savedFile.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this@MainActivity, "保存失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAsZip() {
        val successFiles = files.filter { it.status == FileStatus.SUCCESS && it.lrcContent != null && it.outputFileName != null }

        if (successFiles.isEmpty()) {
            Toast.makeText(this, "沒有可導出的文件", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val filesToZip = successFiles.map { it.outputFileName!! to it.lrcContent!! }
            val zipFileName = StorageHelper.generateZipFileName()

            val zipFile = StorageHelper.saveAsZip(
                this@MainActivity,
                filesToZip,
                zipFileName
            )

            withContext(Dispatchers.Main) {
                if (zipFile != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "已保存 ZIP: ${zipFile.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this@MainActivity, "ZIP 保存失敗", Toast.LENGTH_SHORT).show()
                }
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
            val savedFiles = StorageHelper.saveMultipleFiles(this@MainActivity, filesToSave)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "已保存 ${savedFiles.size} 個文件",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
