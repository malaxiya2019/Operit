package com.ai.assistance.operit.core.avatar.common.control

import com.ai.assistance.operit.core.avatar.common.state.AvatarEmotion

/**
 * Pure validation/normalization helpers for the external avatar control API.
 *
 * The clamp bounds intentionally mirror [DragonBonesAvatarController.updateSettings]
 * (scale 0.1..5.0, translate +-2000) so the HTTP layer applies exactly the same
 * limits as the existing runtime controller. Keeping these as pure functions
 * makes them directly unit-testable without Android/Compose dependencies.
 */
internal object AvatarControlValidation {

    const val SCALE_MIN = 0.1f
    const val SCALE_MAX = 5.0f
    const val TRANSLATE_MIN = -2000f
    const val TRANSLATE_MAX = 2000f

    /**
     * Parses a raw emotion string into an [AvatarEmotion], case-insensitively.
     * Returns null for blank or unknown values.
     */
    fun parseEmotion(raw: String?): AvatarEmotion? {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return null
        }
        return AvatarEmotion.values().firstOrNull { emotion ->
            emotion.name.equals(normalized, ignoreCase = true)
        }
    }

    /** Clamps a scale value to the same bounds used by the runtime controller. */
    fun clampScale(value: Float): Float = value.coerceIn(SCALE_MIN, SCALE_MAX)

    /** Clamps a translate value to the same bounds used by the runtime controller. */
    fun clampTranslate(value: Float): Float = value.coerceIn(TRANSLATE_MIN, TRANSLATE_MAX)

    /**
     * Maps the API loop flag to the controller playback count.
     * `loop = true` -> 0 (infinite looping), `loop = false` -> 1 (play once).
     * This matches the existing usage in FloatingFullscreenScreen.
     */
    fun loopFlagToPlaybackCount(loop: Boolean): Int = if (loop) 0 else 1
}
