package com.ai.assistance.operit.core.avatar.common.control

/**
 * Application-scoped bridge that exposes the currently active [AvatarController]
 * to non-UI callers (e.g. the HTTP avatar API).
 *
 * AvatarController instances are created inside Compose composition
 * (`remember { ... }`), so the HTTP layer cannot reach them directly. The Compose
 * screens that own a controller register/unregister it here; the HTTP layer reads
 * the single active instance. This guarantees the HTTP API drives the exact same
 * controller that is rendering on screen, instead of creating a second one.
 */
object AvatarControlManager {

    @Volatile
    private var activeController: AvatarController? = null

    @Volatile
    private var activeAvatarId: String? = null

    /**
     * Registers [controller] as the currently active avatar controller.
     *
     * @param avatarId The id of the avatar model this controller was created for.
     * @param controller The controller instance currently rendered by the UI.
     */
    fun registerActiveController(avatarId: String?, controller: AvatarController) {
        activeController = controller
        activeAvatarId = avatarId
    }

    /**
     * Unregisters [controller] only when it is still the currently registered
     * instance. If a newer screen already replaced it, the removal is a no-op.
     */
    fun unregisterActiveController(controller: AvatarController) {
        if (activeController === controller) {
            activeController = null
            activeAvatarId = null
        }
    }

    /** Clears the active registration unconditionally. */
    fun clearActiveController() {
        activeController = null
        activeAvatarId = null
    }

    /** The currently active avatar controller, or null when none is rendered. */
    fun getActiveController(): AvatarController? = activeController

    /** The avatar id associated with the active controller, if any. */
    fun getActiveAvatarId(): String? = activeAvatarId
}
