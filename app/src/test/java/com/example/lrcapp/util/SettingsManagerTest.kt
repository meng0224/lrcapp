package com.example.lrcapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsManagerTest {

    @Test
    fun fromStoredValuesLoadsOutputToSourceDirectory() {
        val settings = SettingsManager.fromStoredValues(
            outputDirUri = null,
            outputToSourceDirectory = true
        )

        assertTrue(settings.outputToSourceDirectory)
        assertNull(settings.outputDirUri)
        assertTrue(settings.smartNaming)
        assertTrue(settings.timePrecision)
    }

    @Test
    fun fromStoredValuesKeepsCustomOutputDirectoryWhenSourceModeDisabled() {
        val settings = SettingsManager.fromStoredValues(
            outputDirUri = "content://tree/downloads",
            outputToSourceDirectory = false
        )

        assertEquals("content://tree/downloads", settings.outputDirUri)
        assertFalse(settings.outputToSourceDirectory)
    }

    @Test
    fun sourceDirectoryPreferenceKeyIsStableAndEncoded() {
        val key = SettingsManager.sourceDirectoryPreferenceKey("com.android.externalstorage.documents|primary:Music/Lyrics")

        assertEquals(
            "source_directory_uri_com.android.externalstorage.documents%7Cprimary%3AMusic%2FLyrics",
            key
        )
    }
}
