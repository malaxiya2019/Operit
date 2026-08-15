package com.ai.assistance.operit.integrations.http.bridge

import android.content.Context
import com.ai.assistance.operit.core.avatar.common.control.AvatarControlManager
import com.ai.assistance.operit.core.avatar.common.control.AvatarControlValidation
import com.ai.assistance.operit.core.avatar.common.control.AvatarSettingKeys
import com.ai.assistance.operit.data.repository.AvatarInstanceSettings
import com.ai.assistance.operit.data.repository.AvatarRepository
import com.ai.assistance.operit.core.avatar.impl.factory.AvatarModelFactoryImpl
import com.ai.assistance.operit.integrations.http.WebAvatarStateResponse

/**
 * Result of an avatar control operation.
 *
 * [Success] carries the state snapshot to return to the caller; [Failure] carries
 * the HTTP status code plus a machine-readable error token that follows the
 * existing WebErrorResponse style.
 */
internal sealed interface AvatarControlResult {
    data class Success(val state: WebAvatarStateResponse) : AvatarControlResult
    data class Failure(val httpCode: Int, val error: String) : AvatarControlResult
}

/**
 * Bridge between the HTTP avatar API and the existing avatar control layer.
 *
 * - Runtime control (emotion / animation / immediate settings) goes through the
 *   [AvatarControlManager] active controller, i.e. the exact instance the UI is
 *   currently rendering.
 * - Persistence (avatar settings) goes through [AvatarRepository.updateAvatarSettings].
 *
 * The controller is never created here; if no UI has registered one the caller
 * gets `avatar_not_ready`.
 */
internal class WebChatAvatarBridge(
    appContext: Context
) {

    private val avatarRepository: AvatarRepository by lazy {
        AvatarRepository.getInstance(appContext, AvatarModelFactoryImpl())
    }

    /** GET /api/web/avatar/state */
    fun resolveState(): AvatarControlResult {
        val avatarId = currentAvatarId()
        if (avatarId == null) {
            return notReady()
        }

        val controller = AvatarControlManager.getActiveController()
        val state = controller?.state?.value
        val settings = avatarRepository.getAvatarSettings(avatarId)

        return AvatarControlResult.Success(
            WebAvatarStateResponse(
                avatarId = avatarId,
                emotion = state?.emotion?.name ?: "IDLE",
                animation = state?.currentAnimation,
                isLooping = state?.isLooping ?: false,
                scale = settings.scale,
                translateX = settings.translateX,
                translateY = settings.translateY,
                ready = controller != null
            )
        )
    }

    /** POST /api/web/avatar/emotion */
    fun setEmotion(rawEmotion: String?): AvatarControlResult {
        if (rawEmotion.isNullOrBlank()) {
            return badRequest("missing field: emotion")
        }

        val emotion = AvatarControlValidation.parseEmotion(rawEmotion)
            ?: return badRequest("invalid emotion: $rawEmotion")

        val controller = AvatarControlManager.getActiveController()
            ?: return notReady()

        controller.setEmotion(emotion)
        return resolveState()
    }

    /** POST /api/web/avatar/animation */
    fun playAnimation(animationName: String?, loop: Boolean): AvatarControlResult {
        if (animationName.isNullOrBlank()) {
            return badRequest("missing field: animation")
        }

        val controller = AvatarControlManager.getActiveController()
            ?: return notReady()

        if (!controller.availableAnimations.contains(animationName)) {
            return badRequest("invalid animation: $animationName")
        }

        controller.playAnimation(
            animationName,
            loop = AvatarControlValidation.loopFlagToPlaybackCount(loop)
        )
        return resolveState()
    }

    /** POST /api/web/avatar/settings */
    fun updateSettings(
        scale: Float?,
        translateX: Float?,
        translateY: Float?
    ): AvatarControlResult {
        val avatarId = currentAvatarId()
        if (avatarId == null) {
            return notReady()
        }

        val current = avatarRepository.getAvatarSettings(avatarId)
        val normalizedScale = scale?.let(AvatarControlValidation::clampScale) ?: current.scale
        val normalizedTranslateX =
            translateX?.let(AvatarControlValidation::clampTranslate) ?: current.translateX
        val normalizedTranslateY =
            translateY?.let(AvatarControlValidation::clampTranslate) ?: current.translateY

        // 1) Persist first so the change survives restart.
        avatarRepository.updateAvatarSettings(
            avatarId,
            AvatarInstanceSettings(
                scale = normalizedScale,
                translateX = normalizedTranslateX,
                translateY = normalizedTranslateY,
                customSettings = current.customSettings
            )
        )

        // 2) Apply to the running controller so the on-screen avatar updates immediately.
        val controller = AvatarControlManager.getActiveController()
        if (controller != null) {
            controller.updateSettings(
                mapOf(
                    AvatarSettingKeys.SCALE to normalizedScale,
                    AvatarSettingKeys.TRANSLATE_X to normalizedTranslateX,
                    AvatarSettingKeys.TRANSLATE_Y to normalizedTranslateY
                )
            )
        }

        return resolveState()
    }

    private fun currentAvatarId(): String? {
        return AvatarControlManager.getActiveAvatarId()
            ?: avatarRepository.currentAvatar.value?.id
    }

    private fun notReady(): AvatarControlResult =
        AvatarControlResult.Failure(HTTP_SERVICE_UNAVAILABLE, "avatar_not_ready")

    private fun badRequest(error: String): AvatarControlResult =
        AvatarControlResult.Failure(HTTP_BAD_REQUEST, error)

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_SERVICE_UNAVAILABLE = 503
    }
}
