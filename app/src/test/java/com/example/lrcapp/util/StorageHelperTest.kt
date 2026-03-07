package com.example.lrcapp.util

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageHelperTest {

    @Test
    fun existingDocumentIsReusedWithoutCreatingReplacement() {
        val dir = FakeDocumentFile("dir", true)
        val existing = FakeDocumentFile("song.lrc", false)
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

    private class FakeDocumentFile(
        private val displayName: String,
        private val directory: Boolean,
        private val fileType: String? = null
    ) : DocumentFile(null) {
        val children = mutableListOf<FakeDocumentFile>()
        var createFileCalls = 0
        var deleteCalls = 0

        override fun createFile(mimeType: String, displayName: String): DocumentFile {
            createFileCalls++
            return FakeDocumentFile(displayName, false, mimeType).also { children.add(it) }
        }

        override fun createDirectory(displayName: String): DocumentFile {
            return FakeDocumentFile(displayName, true).also { children.add(it) }
        }

        override fun getUri(): Uri = Uri.parse("content://test/$displayName")

        override fun getName(): String = displayName

        override fun getType(): String? = fileType

        override fun isDirectory(): Boolean = directory

        override fun isFile(): Boolean = !directory

        override fun isVirtual(): Boolean = false

        override fun lastModified(): Long = 0L

        override fun length(): Long = 0L

        override fun canRead(): Boolean = true

        override fun canWrite(): Boolean = true

        override fun delete(): Boolean {
            deleteCalls++
            return true
        }

        override fun exists(): Boolean = true

        override fun listFiles(): Array<DocumentFile> = children.toTypedArray()

        override fun renameTo(displayName: String): Boolean = false
    }
}
