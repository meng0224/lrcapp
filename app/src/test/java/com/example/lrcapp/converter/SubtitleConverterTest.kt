package com.example.lrcapp.converter

import android.test.mock.MockContext
import com.example.lrcapp.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleConverterTest {

    private val converter = SubtitleConverter(MockContext(), AppSettings())

    @Test
    fun assDialogueWithCommaKeepsFullText() {
        val content = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,Hello, world
        """.trimIndent()

        assertEquals("[00:01.23]Hello, world", converter.convertAssToLrc(content))
    }

    @Test
    fun assDialogueTextStillCleansStyleMarkup() {
        val content = """
            [Events]
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,{\\i1}Hello{\\i0}\\Nworld
        """.trimIndent()

        assertEquals("[00:01.23]Hello world", converter.convertAssToLrc(content))
    }
}
