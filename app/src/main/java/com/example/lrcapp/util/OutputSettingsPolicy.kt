package com.example.lrcapp.util

import com.example.lrcapp.model.AppSettings

object OutputSettingsPolicy {
    fun canEnableSourceDirectoryOutput(settings: AppSettings): Boolean {
        return settings.outputDirUri == null
    }

    fun canSelectCustomOutputDirectory(settings: AppSettings): Boolean {
        return !settings.outputToSourceDirectory
    }
}
