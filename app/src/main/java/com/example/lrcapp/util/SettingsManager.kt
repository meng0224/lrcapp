package com.example.lrcapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.lrcapp.model.AppSettings

object SettingsManager {
    private const val PREFS_NAME = "lrc_app_settings"
    private const val KEY_OUTPUT_DIR_URI = "output_dir_uri"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = getSharedPreferences(context)
        return AppSettings(
            smartNaming = true,
            timePrecision = true,
            outputDirUri = prefs.getString(KEY_OUTPUT_DIR_URI, null)
        )
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_OUTPUT_DIR_URI, settings.outputDirUri)
            apply()
        }
    }
}
