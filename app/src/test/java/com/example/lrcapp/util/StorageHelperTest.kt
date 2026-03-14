package com.example.lrcapp.util

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageHelperTest {

    @Test
    fun existingDocumentIsReusedWithoutCreatingReplacement() {
        val dir = FakeDocumentFile("dir", true)
        val existing = FakeDocumentFile("song.lrc", false, length = 16L)
        dir.children.add(existing)

        val resolved = StorageHelper.getOrCreateOutputDocument(dir, "song.lrc", "application/octet-stream")

        assertSame(existing, resolved)
        assertEquals(0, dir.createFileCalls)
        assertEquals(0, existing.deleteCalls)
    }

    @Test
    fun missingDocumentIsCreated() {
        val dir = FakeDocumentFile("dir", true)

        val created = StorageHelper.getOrCreateOutputDocument(dir, "song.lrc", "application/octet-stream")

        assertTrue(created != null)
        assertEquals(1, dir.createFileCalls)
        assertEquals("song.lrc", created?.name)
    }

    @Test
    fun resolveTargetDirectoryReusesExistingNestedDirectories() {
        val root = FakeDocumentFile("root", true)
        val season = FakeDocumentFile("season", true)
        val episode = FakeDocumentFile("episode", true)
        root.children.add(season)
        season.children.add(episode)

        val resolved = StorageHelper.resolveTargetDirectory(root, "season/episode")

        assertSame(episode, resolved)
        assertEquals(0, root.createDirectoryCalls)
        assertEquals(0, season.createDirectoryCalls)
    }

    @Test
    fun resolveTargetDirectoryCreatesMissingNestedDirectories() {
        val root = FakeDocumentFile("root", true)

        val resolved = StorageHelper.resolveTargetDirectory(root, "season/episode")

        assertTrue(resolved != null)
        assertEquals(1, root.createDirectoryCalls)
        assertEquals(1, root.children[0].createDirectoryCalls)
        assertEquals("season", root.children[0].name)
        assertEquals("episode", resolved?.name)
    }

    @Test
    fun verifySavedDocumentRejectsMissingFile() {
        val file = FakeDocumentFile("song.lrc", false, exists = false, length = 16L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertFalse(result)
    }

    @Test
    fun verifySavedDocumentRejectsZeroLengthFile() {
        val file = FakeDocumentFile("song.lrc", false, length = 0L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertFalse(result)
    }

    @Test
    fun verifySavedDocumentAcceptsExpectedBytes() {
        val file = FakeDocumentFile("song.lrc", false, length = 32L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertTrue(result)
    }

    @Test
    fun verifySavedFileNameRejectsProviderAppendedTxt() {
        val result = StorageHelper.verifySavedFileName("song.lrc", "song.lrc.txt")

        assertFalse(result)
    }

    @Test
    fun verifySavedFileNameAcceptsExpectedName() {
        val result = StorageHelper.verifySavedFileName("song.lrc", "song.lrc")

        assertTrue(result)
    }

    @Test
    fun countSuccessfulResultsOnlyCountsNonNullEntries() {
        val savedCount = StorageHelper.countSuccessfulResults(listOf("a.lrc", null, "b.lrc", null))

        assertEquals(2, savedCount)
    }

    @Test
    fun countSuccessfulOutputResultsOnlyCountsVerifiedTargets() {
        val successTarget = StorageHelper.OutputTarget(
            directoryUri = Uri.parse("content://tree/one"),
            fileName = "a.lrc",
            content = "[00:00.00]A",
            fileIndex = 0,
            relativeDirectoryPath = "album/disc1"
        )
        val failedTarget = StorageHelper.OutputTarget(
            directoryUri = Uri.parse("content://tree/two"),
            fileName = "b.lrc",
            content = "[00:00.00]B",
            fileIndex = 1
        )

        val count = StorageHelper.countSuccessfulOutputResults(
            listOf(
                StorageHelper.OutputResult(successTarget, Uri.parse("content://tree/one/a"), "a.lrc", 16L),
                StorageHelper.OutputResult(failedTarget, null, "b.lrc.txt", 0L)
            )
        )

        assertEquals(1, count)
    }

    private class FakeDocumentFile(
        private val displayName: String,
        private val directory: Boolean,
        private val fileType: String? = null,
        private val exists: Boolean = true,
        private val length: Long = 0L
    ) : DocumentFile(null) {
        val children = mutableListOf<FakeDocumentFile>()
        var createFileCalls = 0
        var createDirectoryCalls = 0
        var deleteCalls = 0

        override fun createFile(mimeType: String, displayName: String): DocumentFile {
            createFileCalls++
            return FakeDocumentFile(displayName, false, mimeType, length = 16L).also { children.add(it) }
        }

        override fun createDirectory(displayName: String): DocumentFile {
            createDirectoryCalls++
            return FakeDocumentFile(displayName, true).also { children.add(it) }
        }

        override fun getUri(): Uri = Uri.parse("content://test/$displayName")

        override fun getName(): String = displayName

        override fun getType(): String? = fileType

        override fun isDirectory(): Boolean = directory

        override fun isFile(): Boolean = !directory

        override fun isVirtual(): Boolean = false

        override fun lastModified(): Long = 0L

        override fun length(): Long = length

        override fun canRead(): Boolean = true

        override fun canWrite(): Boolean = true

        override fun delete(): Boolean {
            deleteCalls++
            return true
        }

        override fun exists(): Boolean = exists

        override fun listFiles(): Array<DocumentFile> = children.toTypedArray()

        override fun findFile(displayName: String): DocumentFile? {
            return children.firstOrNull { it.name == displayName }
        }

        override fun renameTo(displayName: String): Boolean = false
    }
}
