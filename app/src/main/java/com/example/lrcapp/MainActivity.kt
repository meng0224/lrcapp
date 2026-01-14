package com.example.lrcapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lrcapp.adapter.SubtitleFileAdapter
import com.example.lrcapp.converter.SubtitleConverter
import com.example.lrcapp.databinding.ActivityMainBinding
import com.example.lrcapp.model.AppSettings
import com.example.lrcapp.model.FileStatus
import com.example.lrcapp.model.SubtitleFile
import com.example.lrcapp.util.FileNameHelper
import com.example.lrcapp.util.FileValidator
import com.example.lrcapp.util.SettingsManager
import com.example.lrcapp.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SubtitleFileAdapter

    private val files = mutableListOf<SubtitleFile>()
    private var settings = AppSettings()
    private var openPickerOnPermissionGrant = false

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
            if (openPickerOnPermissionGrant) {
                openFilePicker()
            }
        } else {
            Toast.makeText(this, "未授予讀取外部儲存的權限", Toast.LENGTH_SHORT).show()
        }
        openPickerOnPermissionGrant = false
    }

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (openPickerOnPermissionGrant) {
                    openFilePicker()
                }
            } else {
                Toast.makeText(this, "未授予所有檔案存取權", Toast.LENGTH_SHORT).show()
            }
            openPickerOnPermissionGrant = false
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupRecyclerView()
        setupClickListeners()
        checkAndRequestPermissions()
    }

    private fun loadSettings() {
        settings = SettingsManager.loadSettings(this)
        binding.switchSmartNaming.isChecked = settings.smartNaming
        binding.switchTimePrecision.isChecked = settings.timePrecision
        updateOutputDirDisplay()
    }

    private fun setupRecyclerView() {
        adapter = SubtitleFileAdapter(files) { file ->
            downloadSingleFile(file)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSelectFiles.setOnClickListener {
            openPickerOnPermissionGrant = true
            checkAndRequestPermissions()
        }

        binding.btnConvert.setOnClickListener {
            if (files.isEmpty()) {
                Toast.makeText(this, "請先選擇文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startConversion()
        }

        binding.btnDownloadZip.setOnClickListener {
            downloadAsZip()
        }

        binding.btnDownloadAll.setOnClickListener {
            downloadAllFiles()
        }

        binding.btnSelectOutputDir.setOnClickListener {
            directoryPickerLauncher.launch(null)
        }

        binding.switchSmartNaming.setOnCheckedChangeListener { _, isChecked ->
            settings.smartNaming = isChecked
            SettingsManager.saveSettings(this, settings)
        }

        binding.switchTimePrecision.setOnCheckedChangeListener { _, isChecked ->
            settings.timePrecision = isChecked
            SettingsManager.saveSettings(this, settings)
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                settingsLauncher.launch(intent)
            } else {
                if (openPickerOnPermissionGrant) {
                    openFilePicker()
                    openPickerOnPermissionGrant = false
                }
            }
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(permission))
            } else {
                if (openPickerOnPermissionGrant) {
                    openFilePicker()
                    openPickerOnPermissionGrant = false
                }
            }
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
            val newFiles = uris.mapNotNull { uri ->
                try {
                    val fileName = getFileName(uri)
                    val fileSize = getFileSize(uri)
                    val (isValid, errorMessage) = FileValidator.validateFile(fileName, fileSize)
                    SubtitleFile(
                        uri = uri,
                        fileName = fileName,
                        fileSize = fileSize,
                        status = if (isValid) FileStatus.PENDING else FileStatus.ERROR,
                        errorMessage = errorMessage
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
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
        return DocumentFile.fromSingleUri(this, uri)?.name ?: "未知文件"
    }

    private fun getFileSize(uri: Uri): Long {
        return DocumentFile.fromSingleUri(this, uri)?.length() ?: 0L
    }

    private fun startConversion() {
        settings = SettingsManager.loadSettings(this)
        binding.progressBar.progress = 0
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        binding.exportButtonsLayout.visibility = View.GONE
        val filesToProcess = files.filter { it.status == FileStatus.PENDING || it.status == FileStatus.ERROR }
        if (filesToProcess.isEmpty()) {
            Toast.makeText(this, "沒有需要轉換的文件", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.tvProgress.visibility = View.GONE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val converter = SubtitleConverter(this@MainActivity, settings)
            var processedCount = 0

            filesToProcess.forEach { file ->
                val fileIndex = files.indexOf(file)
                if (fileIndex < 0) return@forEach
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
                    binding.progressBar.progress = progress
                    binding.tvProgress.text = "進度: $progress%"
                }
            }
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.tvProgress.visibility = View.GONE
                val successCount = files.count { it.status == FileStatus.SUCCESS }
                if (successCount > 0) {
                    binding.exportButtonsLayout.visibility = View.VISIBLE
                }
                Toast.makeText(this@MainActivity, "轉換完成！成功: $successCount / ${filesToProcess.size}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadSingleFile(file: SubtitleFile) {
        if (file.lrcContent == null || file.outputFileName == null) {
            Toast.makeText(this, "文件內容為空", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val outputDirUri = settings.outputDirUri?.let { Uri.parse(it) }
            val savedFile = StorageHelper.saveLrcFile(this@MainActivity, outputDirUri, file.outputFileName!!, file.lrcContent!!)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, if (savedFile != null) "已保存: $savedFile" else "保存失敗", Toast.LENGTH_SHORT).show()
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
            val outputDirUri = settings.outputDirUri?.let { Uri.parse(it) }
            val zipFile = StorageHelper.saveAsZip(this@MainActivity, outputDirUri, filesToZip, zipFileName)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, if (zipFile != null) "已保存 ZIP: $zipFile" else "ZIP 保存失敗", Toast.LENGTH_SHORT).show()
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
            binding.tvOutputDir.text = "儲存位置: ${docFile?.name}"
        } else {
            binding.tvOutputDir.text = "儲存位置: 預設下載目錄"
        }
    }
}
