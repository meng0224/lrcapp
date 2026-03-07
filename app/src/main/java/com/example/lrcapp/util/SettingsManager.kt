package com.example.lrcapp.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.lrcapp.model.AppSettings

object SettingsManager {
    private const val PREFS_NAME = "lrc_app_settings"
    private const val KEY_OUTPUT_DIR_URI = "output_dir_uri"
    private const val KEY_OUTPUT_TO_SOURCE_DIRECTORY = "output_to_source_directory"
    private const val KEY_SOURCE_DIRECTORY_URI_PREFIX = "source_directory_uri_"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = getSharedPreferences(context)
        return fromStoredValues(
            outputDirUri = prefs.getString(KEY_OUTPUT_DIR_URI, null),
            outputToSourceDirectory = prefs.getBoolean(KEY_OUTPUT_TO_SOURCE_DIRECTORY, false)
        )
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_OUTPUT_DIR_URI, settings.outputDirUri)
            putBoolean(KEY_OUTPUT_TO_SOURCE_DIRECTORY, settings.outputToSourceDirectory)
            apply()
        }
    }

    fun getSourceDirectoryUri(context: Context, sourceDirectoryKey: String): String? {
        return getSharedPreferences(context).getString(sourceDirectoryPreferenceKey(sourceDirectoryKey), null)
    }

    fun saveSourceDirectoryUri(context: Context, sourceDirectoryKey: String, treeUri: String) {
        getSharedPreferences(context).edit()
            .putString(sourceDirectoryPreferenceKey(sourceDirectoryKey), treeUri)
            .apply()
    }

    internal fun fromStoredValues(outputDirUri: String?, outputToSourceDirectory: Boolean): AppSettings {
        return AppSettings(
            smartNaming = true,
            timePrecision = true,
            outputDirUri = outputDirUri,
            outputToSourceDirectory = outputToSourceDirectory
        )
    }

    internal fun sourceDirectoryPreferenceKey(sourceDirectoryKey: String): String {
        return KEY_SOURCE_DIRECTORY_URI_PREFIX + Uri.encode(sourceDirectoryKey)
    }
}
