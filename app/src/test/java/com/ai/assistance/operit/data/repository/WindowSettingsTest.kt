package com.ai.assistance.operit.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WindowSettings] — the configurable fullscreen window sizes.
 *
 * Verifies the defaults mirror the original hard-coded values in
 * FloatingFullscreenScreen (420/320/220 for voice-avatar, 300/120/140 otherwise)
 * and that [WindowSettings.mergedWith] performs a true partial update.
 */
class WindowSettingsTest {

    @Test
    fun defaults_matchOriginalHardCodedSizes() {
        val settings = WindowSettings()
        assertEquals(420f, settings.waveSizeVoiceDp, 0.0f)
        assertEquals(320f, settings.avatarSizeVoiceDp, 0.0f)
        assertEquals(220f, settings.tapTargetVoiceDp, 0.0f)
        assertEquals(300f, settings.waveSizePlainDp, 0.0f)
        assertEquals(120f, settings.avatarSizePlainDp, 0.0f)
        assertEquals(140f, settings.tapTargetPlainDp, 0.0f)
    }

    @Test
    fun mergedWith_onlyReplacesProvidedFields() {
        val base = WindowSettings()
        val merged = base.mergedWith(waveSizeVoice = 360f, tapTargetPlain = 200f)

        assertEquals(360f, merged.waveSizeVoiceDp, 0.0f)
        assertEquals(320f, merged.avatarSizeVoiceDp, 0.0f)
        assertEquals(220f, merged.tapTargetVoiceDp, 0.0f)
        assertEquals(300f, merged.waveSizePlainDp, 0.0f)
        assertEquals(120f, merged.avatarSizePlainDp, 0.0f)
        assertEquals(200f, merged.tapTargetPlainDp, 0.0f)
    }

    @Test
    fun mergedWith_nullFields_keepsCurrentValues() {
        val base = WindowSettings().mergedWith(waveSizeVoice = 500f, avatarSizeVoice = 250f)
        val merged = base.mergedWith(waveSizeVoice = null, avatarSizePlain = 180f)

        assertEquals(500f, merged.waveSizeVoiceDp, 0.0f)
        assertEquals(250f, merged.avatarSizeVoiceDp, 0.0f)
        assertEquals(180f, merged.waveSizePlainDp, 0.0f)
        assertEquals(120f, merged.avatarSizePlainDp, 0.0f)
    }

    @Test
    fun mergedWith_allFields_producesFullReplacement() {
        val merged = WindowSettings().mergedWith(
            waveSizeVoice = 100f,
            avatarSizeVoice = 60f,
            tapTargetVoice = 50f,
            waveSizePlain = 110f,
            avatarSizePlain = 70f,
            tapTargetPlain = 80f
        )
        assertEquals(100f, merged.waveSizeVoiceDp, 0.0f)
        assertEquals(60f, merged.avatarSizeVoiceDp, 0.0f)
        assertEquals(50f, merged.tapTargetVoiceDp, 0.0f)
        assertEquals(110f, merged.waveSizePlainDp, 0.0f)
        assertEquals(70f, merged.avatarSizePlainDp, 0.0f)
        assertEquals(80f, merged.tapTargetPlainDp, 0.0f)
    }

    @Test
    fun mergedWith_isNonMutating() {
        val base = WindowSettings()
        base.mergedWith(waveSizeVoice = 999f)
        // 原对象保持不变（data class 不可变）
        assertEquals(420f, base.waveSizeVoiceDp, 0.0f)
    }
}
