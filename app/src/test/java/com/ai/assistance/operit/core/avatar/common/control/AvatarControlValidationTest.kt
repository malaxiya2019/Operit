package com.ai.assistance.operit.core.avatar.common.control

import com.ai.assistance.operit.core.avatar.common.state.AvatarEmotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AvatarControlValidation] — the pure normalization helpers
 * shared by the external avatar control API.
 */
class AvatarControlValidationTest {

    @Test
    fun parseEmotion_lowercase_happy_returnsHappy() {
        assertEquals(AvatarEmotion.HAPPY, AvatarControlValidation.parseEmotion("happy"))
    }

    @Test
    fun parseEmotion_uppercase_surprised_returnsSurprised() {
        assertEquals(AvatarEmotion.SURPRISED, AvatarControlValidation.parseEmotion("SURPRISED"))
    }

    @Test
    fun parseEmotion_mixedCase_thinking_returnsThinking() {
        assertEquals(AvatarEmotion.THINKING, AvatarControlValidation.parseEmotion("Thinking"))
    }

    @Test
    fun parseEmotion_allSupportedEmotions_areParsed() {
        for (emotion in AvatarEmotion.values()) {
            assertEquals(emotion, AvatarControlValidation.parseEmotion(emotion.name))
            assertEquals(emotion, AvatarControlValidation.parseEmotion(emotion.name.lowercase()))
        }
    }

    @Test
    fun parseEmotion_unknownValue_returnsNull() {
        assertNull(AvatarControlValidation.parseEmotion("ecstatic"))
    }

    @Test
    fun parseEmotion_blank_returnsNull() {
        assertNull(AvatarControlValidation.parseEmotion(""))
        assertNull(AvatarControlValidation.parseEmotion("   "))
        assertNull(AvatarControlValidation.parseEmotion(null))
    }

    @Test
    fun clampScale_withinRange_isUnchanged() {
        assertEquals(1.5f, AvatarControlValidation.clampScale(1.5f), 0.0f)
        assertEquals(1.0f, AvatarControlValidation.clampScale(1.0f), 0.0f)
    }

    @Test
    fun clampScale_aboveMax_isClampedToMax() {
        assertEquals(5.0f, AvatarControlValidation.clampScale(99f), 0.0f)
        assertEquals(5.0f, AvatarControlValidation.clampScale(5.1f), 0.0f)
    }

    @Test
    fun clampScale_belowMin_isClampedToMin() {
        assertEquals(0.1f, AvatarControlValidation.clampScale(0f), 0.0f)
        assertEquals(0.1f, AvatarControlValidation.clampScale(-2f), 0.0f)
    }

    @Test
    fun clampTranslate_withinRange_isUnchanged() {
        assertEquals(100f, AvatarControlValidation.clampTranslate(100f), 0.0f)
        assertEquals(-50f, AvatarControlValidation.clampTranslate(-50f), 0.0f)
    }

    @Test
    fun clampTranslate_aboveMax_isClamped() {
        assertEquals(2000f, AvatarControlValidation.clampTranslate(5000f), 0.0f)
    }

    @Test
    fun clampTranslate_belowMin_isClamped() {
        assertEquals(-2000f, AvatarControlValidation.clampTranslate(-5000f), 0.0f)
    }

    @Test
    fun clampWaveSize_withinRange_isUnchanged() {
        assertEquals(420f, AvatarControlValidation.clampWaveSize(420f), 0.0f)
        assertEquals(100f, AvatarControlValidation.clampWaveSize(100f), 0.0f)
        assertEquals(2000f, AvatarControlValidation.clampWaveSize(2000f), 0.0f)
    }

    @Test
    fun clampWaveSize_outOfRange_isClamped() {
        assertEquals(100f, AvatarControlValidation.clampWaveSize(10f), 0.0f)
        assertEquals(2000f, AvatarControlValidation.clampWaveSize(9999f), 0.0f)
        assertEquals(100f, AvatarControlValidation.clampWaveSize(-50f), 0.0f)
    }

    @Test
    fun clampAvatarSize_withinRange_isUnchanged() {
        assertEquals(320f, AvatarControlValidation.clampAvatarSize(320f), 0.0f)
        assertEquals(40f, AvatarControlValidation.clampAvatarSize(40f), 0.0f)
        assertEquals(1000f, AvatarControlValidation.clampAvatarSize(1000f), 0.0f)
    }

    @Test
    fun clampAvatarSize_outOfRange_isClamped() {
        assertEquals(40f, AvatarControlValidation.clampAvatarSize(5f), 0.0f)
        assertEquals(1000f, AvatarControlValidation.clampAvatarSize(5000f), 0.0f)
    }

    @Test
    fun clampTapTarget_withinRange_isUnchanged() {
        assertEquals(220f, AvatarControlValidation.clampTapTarget(220f), 0.0f)
        assertEquals(40f, AvatarControlValidation.clampTapTarget(40f), 0.0f)
        assertEquals(1000f, AvatarControlValidation.clampTapTarget(1000f), 0.0f)
    }

    @Test
    fun clampTapTarget_outOfRange_isClamped() {
        assertEquals(40f, AvatarControlValidation.clampTapTarget(0f), 0.0f)
        assertEquals(1000f, AvatarControlValidation.clampTapTarget(3000f), 0.0f)
    }

    @Test
    fun loopFlagToPlaybackCount_true_meansInfiniteLoop() {
        assertEquals(0, AvatarControlValidation.loopFlagToPlaybackCount(true))
    }

    @Test
    fun loopFlagToPlaybackCount_false_meansPlayOnce() {
        assertEquals(1, AvatarControlValidation.loopFlagToPlaybackCount(false))
    }
}
