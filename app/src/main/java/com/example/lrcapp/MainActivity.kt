package com.example.lrcapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
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
import com.example.lrcapp.util.FileSelectionPolicy
import com.example.lrcapp.util.FileValidator
import com.example.lrcapp.util.OutputSettingsPolicy
import com.example.lrcapp.util.SettingsManager
import com.example.lrcapp.util.StorageHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubtitleFileAdapter
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgress: TextView
    private lateinit var btnSelectFiles: MaterialButton
    private lateinit var btnConvert: MaterialButton
    private lateinit var tvOutputDir: TextView
    private lateinit var btnSelectOutputDir: MaterialButton
    private lateinit var btnClearOutputDir: MaterialButton
    private lateinit var switchOutputToSourceDirectory: SwitchMaterial
    private lateinit var toolbar: MaterialToolbar

    private val files = mutableListOf<SubtitleFile>()
    private var settings = AppSettings()

    private val pendingSourceAuthorizationKeys = ArrayDeque<String>()
    private val pendingSourceAuthorizationLabels = mutableMapOf<String, String>()
    private val pendingSourceReadyTargets = mutableListOf<StorageHelper.OutputTarget>()
    private val pendingSourceOutputs = mutableListOf<PendingSourceOutput>()
    private val pendingSourceSaveFailures = mutableMapOf<Int, String>()
    private var currentSourceAuthorizationKey: String? = null

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
        val sourceAuthorizationKey = currentSourceAuthorizationKey
        if (sourceAuthorizationKey != null) {
            handleSourceDirectoryAuthorizationResult(sourceAuthorizationKey, uri)
        } else if (uri != null) {
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
        btnClearOutputDir = findViewById(R.id.btnClearOutputDir)
        switchOutputToSourceDirectory = findViewById(R.id.switchOutputToSourceDirectory)
    }

    private fun loadSettings() {
        settings = SettingsManager.loadSettings(this)
        if (settings.outputDirUri != null && settings.outputToSourceDirectory) {
            settings.outputToSourceDirectory = false
            SettingsManager.saveSettings(this, settings)
        }
        syncSourceDirectorySwitch()
        updateOutputDirDisplay()
    }

    private fun syncSourceDirectorySwitch() {
        switchOutputToSourceDirectory.setOnCheckedChangeListener(null)
        switchOutputToSourceDirectory.isChecked = settings.outputToSourceDirectory
        switchOutputToSourceDirectory.setOnCheckedChangeListener { _, isChecked ->
            handleSourceDirectoryToggle(isChecked)
        }
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
            if (!OutputSettingsPolicy.canSelectCustomOutputDirectory(settings)) {
                Toast.makeText(this, "已開啟輸出到原文件目錄，請先關閉其中一項", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            directoryPickerLauncher.launch(null)
        }

        btnClearOutputDir.setOnClickListener {
            clearCustomOutputDirectory()
        }
    }

    private fun handleSourceDirectoryToggle(enable: Boolean) {
        if (enable && !OutputSettingsPolicy.canEnableSourceDirectoryOutput(settings)) {
            Toast.makeText(this, "已設定自訂輸出資料夾，請先清除或關閉其中一項", Toast.LENGTH_SHORT).show()
            syncSourceDirectorySwitch()
            return
        }

        settings.outputToSourceDirectory = enable
        SettingsManager.saveSettings(this, settings)
        syncSourceDirectorySwitch()
        updateOutputDirDisplay()
    }

    private fun clearCustomOutputDirectory() {
        if (settings.outputDirUri == null) {
            Toast.makeText(this, "目前沒有自訂輸出資料夾", Toast.LENGTH_SHORT).show()
            return
        }

        settings.outputDirUri = null
        SettingsManager.saveSettings(this, settings)
        updateOutputDirDisplay()
        Toast.makeText(this, "已清除自訂輸出資料夾", Toast.LENGTH_SHORT).show()
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
                    val sourceDirectoryInfo = resolveSourceDirectoryInfo(uri)

                    val (isValid, errorMessage) = FileValidator.validateFile(fileName, fileSize)

                    newFiles.add(
                        SubtitleFile(
                            uri = uri,
                            fileName = fileName,
                            fileSize = fileSize,
                            status = if (isValid) FileStatus.PENDING else FileStatus.INVALID,
                            errorMessage = errorMessage,
                            sourceDirectoryKey = sourceDirectoryInfo?.key,
                            sourceDirectoryLabel = sourceDirectoryInfo?.label
                        )
                    )
                } catch (_: Exception) {
                }
            }

            withContext(Dispatchers.Main) {
                val mergeResult = FileSelectionPolicy.mergeSelections(
                    existingFiles = files,
                    newFiles = newFiles,
                    appendToExisting = settings.outputToSourceDirectory
                )

                files.clear()
                files.addAll(mergeResult.files)
                adapter.notifyDataSetChanged()

                val message = if (settings.outputToSourceDirectory) {
                    "已新增 ${mergeResult.addedCount} 個文件，略過 ${mergeResult.skippedDuplicateCount} 個重複文件"
                } else {
                    "已選擇 ${newFiles.size} 個文件"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
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
                        throw IllegalStateException("解析錯誤或內容為空")
                    }
                } catch (e: Exception) {
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
                Toast.makeText(
                    this@MainActivity,
                    "轉換完成！成功: $successCount / ${filesToProcess.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadAllFiles() {
        val successFiles = files.filter {
            it.status == FileStatus.SUCCESS && it.lrcContent != null && it.outputFileName != null
        }
        if (successFiles.isEmpty()) {
            Toast.makeText(this, "沒有可導出的文件", Toast.LENGTH_SHORT).show()
            return
        }

        if (settings.outputToSourceDirectory) {
            downloadAllFilesToSourceDirectories(successFiles)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val filesToSave = successFiles.map { it.outputFileName!! to it.lrcContent!! }
            val outputDirUri = settings.outputDirUri?.let(Uri::parse)
            val savedFiles = StorageHelper.saveMultipleFiles(this@MainActivity, outputDirUri, filesToSave)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "已保存 $savedFiles 個文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadAllFilesToSourceDirectories(successFiles: List<SubtitleFile>) {
        resetPendingSourceSaveState()

        successFiles.forEach { file ->
            val fileIndex = files.indexOf(file)
            if (fileIndex < 0) {
                return@forEach
            }

            val sourceDirectoryKey = file.sourceDirectoryKey
            if (sourceDirectoryKey == null) {
                pendingSourceSaveFailures[fileIndex] = "保存失敗: 無法判定來源目錄"
                return@forEach
            }

            val savedTreeUri = SettingsManager.getSourceDirectoryUri(this, sourceDirectoryKey)?.let(Uri::parse)
            if (savedTreeUri != null && matchesSourceDirectoryKey(savedTreeUri, sourceDirectoryKey)) {
                pendingSourceReadyTargets.add(
                    StorageHelper.OutputTarget(
                        directoryUri = savedTreeUri,
                        fileName = file.outputFileName!!,
                        content = file.lrcContent!!,
                        fileIndex = fileIndex,
                        sourceDirectoryKey = sourceDirectoryKey
                    )
                )
            } else {
                pendingSourceOutputs.add(
                    PendingSourceOutput(
                        fileIndex = fileIndex,
                        sourceDirectoryKey = sourceDirectoryKey,
                        sourceDirectoryLabel = file.sourceDirectoryLabel ?: "來源目錄",
                        fileName = file.outputFileName!!,
                        content = file.lrcContent!!
                    )
                )
                pendingSourceAuthorizationLabels[sourceDirectoryKey] = file.sourceDirectoryLabel ?: "來源目錄"
            }
        }

        if (pendingSourceOutputs.isEmpty()) {
            savePendingSourceTargets()
            return
        }

        pendingSourceOutputs.map { it.sourceDirectoryKey }
            .distinct()
            .forEach { pendingSourceAuthorizationKeys.add(it) }

        requestNextSourceDirectoryAuthorization()
    }

    private fun requestNextSourceDirectoryAuthorization() {
        if (pendingSourceAuthorizationKeys.isEmpty()) {
            currentSourceAuthorizationKey = null
            savePendingSourceTargets()
            return
        }

        currentSourceAuthorizationKey = pendingSourceAuthorizationKeys.removeFirst()
        val label = pendingSourceAuthorizationLabels[currentSourceAuthorizationKey] ?: "來源目錄"
        Toast.makeText(this, "請授權來源目錄：$label", Toast.LENGTH_SHORT).show()
        directoryPickerLauncher.launch(null)
    }

    private fun handleSourceDirectoryAuthorizationResult(sourceDirectoryKey: String, treeUri: Uri?) {
        if (treeUri == null) {
            markPendingOutputsForSourceDirectory(sourceDirectoryKey, "保存失敗: 未授權來源目錄")
            currentSourceAuthorizationKey = null
            requestNextSourceDirectoryAuthorization()
            return
        }

        if (!matchesSourceDirectoryKey(treeUri, sourceDirectoryKey)) {
            Toast.makeText(this, "選取的目錄與來源目錄不符，請重新選擇", Toast.LENGTH_SHORT).show()
            directoryPickerLauncher.launch(null)
            return
        }

        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        SettingsManager.saveSourceDirectoryUri(this, sourceDirectoryKey, treeUri.toString())
        movePendingOutputsToReadyTargets(sourceDirectoryKey, treeUri)
        currentSourceAuthorizationKey = null
        requestNextSourceDirectoryAuthorization()
    }

    private fun movePendingOutputsToReadyTargets(sourceDirectoryKey: String, treeUri: Uri) {
        val iterator = pendingSourceOutputs.iterator()
        while (iterator.hasNext()) {
            val pendingOutput = iterator.next()
            if (pendingOutput.sourceDirectoryKey == sourceDirectoryKey) {
                pendingSourceReadyTargets.add(
                    StorageHelper.OutputTarget(
                        directoryUri = treeUri,
                        fileName = pendingOutput.fileName,
                        content = pendingOutput.content,
                        fileIndex = pendingOutput.fileIndex,
                        sourceDirectoryKey = sourceDirectoryKey
                    )
                )
                iterator.remove()
            }
        }
    }

    private fun markPendingOutputsForSourceDirectory(sourceDirectoryKey: String, errorMessage: String) {
        val iterator = pendingSourceOutputs.iterator()
        while (iterator.hasNext()) {
            val pendingOutput = iterator.next()
            if (pendingOutput.sourceDirectoryKey == sourceDirectoryKey) {
                pendingSourceSaveFailures[pendingOutput.fileIndex] = errorMessage
                iterator.remove()
            }
        }
    }

    private fun savePendingSourceTargets() {
        val readyTargets = pendingSourceReadyTargets.toList()
        val initialFailures = pendingSourceSaveFailures.toMap()

        if (readyTargets.isEmpty()) {
            applySaveResults(emptyList(), initialFailures)
            resetPendingSourceSaveState()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val results = StorageHelper.saveOutputTargets(this@MainActivity, readyTargets)
            withContext(Dispatchers.Main) {
                applySaveResults(results, initialFailures)
                resetPendingSourceSaveState()
            }
        }
    }

    private fun applySaveResults(
        results: List<StorageHelper.OutputResult>,
        initialFailures: Map<Int, String>
    ) {
        initialFailures.forEach { (fileIndex, errorMessage) ->
            if (fileIndex in files.indices) {
                files[fileIndex].status = FileStatus.ERROR
                files[fileIndex].errorMessage = errorMessage
                adapter.updateFile(fileIndex, files[fileIndex])
            }
        }

        results.forEach { result ->
            if (!result.isSuccess) {
                val fileIndex = result.target.fileIndex
                if (fileIndex in files.indices) {
                    files[fileIndex].status = FileStatus.ERROR
                    files[fileIndex].errorMessage = "保存失敗: 無法寫入原文件目錄"
                    adapter.updateFile(fileIndex, files[fileIndex])
                }
            }
        }

        val successCount = StorageHelper.countSuccessfulOutputResults(results)
        val failureCount = initialFailures.size + results.count { !it.isSuccess }
        val message = if (failureCount > 0) {
            "已保存 $successCount 個文件，失敗 $failureCount 個"
        } else {
            "已保存 $successCount 個文件"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun resetPendingSourceSaveState() {
        pendingSourceAuthorizationKeys.clear()
        pendingSourceAuthorizationLabels.clear()
        pendingSourceReadyTargets.clear()
        pendingSourceOutputs.clear()
        pendingSourceSaveFailures.clear()
        currentSourceAuthorizationKey = null
    }

    private fun handleDirectorySelection(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        settings.outputDirUri = uri.toString()
        SettingsManager.saveSettings(this, settings)
        updateOutputDirDisplay()
        Toast.makeText(this, "已設定自訂輸出資料夾", Toast.LENGTH_SHORT).show()
    }

    private fun updateOutputDirDisplay() {
        val uriString = settings.outputDirUri
        tvOutputDir.text = when {
            settings.outputToSourceDirectory -> "儲存位置: 原文件目錄（逐目錄授權）"
            uriString != null -> {
                val uri = Uri.parse(uriString)
                val docFile = DocumentFile.fromTreeUri(this, uri)
                "儲存位置: ${docFile?.name ?: "未命名目錄"}（SAF 授權目錄）"
            }
            else -> "儲存位置: 預設應用下載目錄"
        }
        btnClearOutputDir.isEnabled = settings.outputDirUri != null
    }

    private fun resolveSourceDirectoryInfo(uri: Uri): SourceDirectoryInfo? {
        if (!DocumentsContract.isDocumentUri(this, uri)) {
            return null
        }

        val authority = uri.authority ?: return null
        val documentId = DocumentsContract.getDocumentId(uri)
        val parentDocumentId = extractParentDocumentId(documentId) ?: return null
        return SourceDirectoryInfo(
            key = "$authority|$parentDocumentId",
            label = extractSourceDirectoryLabel(parentDocumentId)
        )
    }

    private fun extractParentDocumentId(documentId: String): String? {
        val colonIndex = documentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = documentId.substring(0, colonIndex)
            val path = documentId.substring(colonIndex + 1)
            val parentPath = path.substringBeforeLast('/', "")
            return "$root:$parentPath"
        }

        return documentId.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }
    }

    private fun extractSourceDirectoryLabel(parentDocumentId: String): String {
        val colonIndex = parentDocumentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = parentDocumentId.substring(0, colonIndex)
            val path = parentDocumentId.substring(colonIndex + 1)
            return path.substringAfterLast('/', root).ifEmpty { root }
        }
        return parentDocumentId.substringAfterLast('/').ifEmpty { parentDocumentId }
    }

    private fun matchesSourceDirectoryKey(treeUri: Uri, sourceDirectoryKey: String): Boolean {
        val separatorIndex = sourceDirectoryKey.indexOf('|')
        if (separatorIndex <= 0) {
            return false
        }

        val expectedAuthority = sourceDirectoryKey.substring(0, separatorIndex)
        val expectedTreeDocumentId = sourceDirectoryKey.substring(separatorIndex + 1)
        return try {
            treeUri.authority == expectedAuthority &&
                DocumentsContract.getTreeDocumentId(treeUri) == expectedTreeDocumentId
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private data class SourceDirectoryInfo(
        val key: String,
        val label: String
    )

    private data class PendingSourceOutput(
        val fileIndex: Int,
        val sourceDirectoryKey: String,
        val sourceDirectoryLabel: String,
        val fileName: String,
        val content: String
    )
}


