package com.ai.assistance.operit.core.avatar.common.control

import com.ai.assistance.operit.core.avatar.common.state.AvatarEmotion
import com.ai.assistance.operit.core.avatar.common.state.AvatarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AvatarControlManager] — the application-scoped bridge that
 * exposes the currently rendered AvatarController to the HTTP layer.
 */
class AvatarControlManagerTest {

    private val firstController = FakeAvatarController()
    private val secondController = FakeAvatarController()

    @Before
    fun setUp() {
        AvatarControlManager.clearActiveController()
    }

    @After
    fun tearDown() {
        AvatarControlManager.clearActiveController()
    }

    @Test
    fun initially_thereIsNoActiveController() {
        assertNull(AvatarControlManager.getActiveController())
        assertNull(AvatarControlManager.getActiveAvatarId())
    }

    @Test
    fun registerActiveController_exposesControllerAndAvatarId() {
        AvatarControlManager.registerActiveController("avatar-1", firstController)

        assertSame(firstController, AvatarControlManager.getActiveController())
        assertEquals("avatar-1", AvatarControlManager.getActiveAvatarId())
    }

    @Test
    fun registerActiveController_replacesPreviousRegistration() {
        AvatarControlManager.registerActiveController("avatar-1", firstController)
        AvatarControlManager.registerActiveController("avatar-2", secondController)

        assertSame(secondController, AvatarControlManager.getActiveController())
        assertEquals("avatar-2", AvatarControlManager.getActiveAvatarId())
    }

    @Test
    fun unregisterActiveController_clearsWhenItMatchesCurrent() {
        AvatarControlManager.registerActiveController("avatar-1", firstController)
        AvatarControlManager.unregisterActiveController(firstController)

        assertNull(AvatarControlManager.getActiveController())
        assertNull(AvatarControlManager.getActiveAvatarId())
    }

    @Test
    fun unregisterActiveController_keepsNewerControllerWhenGivenStaleOne() {
        // Simulates an old screen disposing while a newer controller is active.
        AvatarControlManager.registerActiveController("avatar-1", firstController)
        AvatarControlManager.registerActiveController("avatar-2", secondController)
        AvatarControlManager.unregisterActiveController(firstController)

        assertSame(secondController, AvatarControlManager.getActiveController())
        assertEquals("avatar-2", AvatarControlManager.getActiveAvatarId())
    }

    @Test
    fun clearActiveController_removesEverything() {
        AvatarControlManager.registerActiveController("avatar-1", firstController)
        AvatarControlManager.clearActiveController()

        assertNull(AvatarControlManager.getActiveController())
        assertNull(AvatarControlManager.getActiveAvatarId())
    }

    private class FakeAvatarController : AvatarController {
        private val _state = MutableStateFlow(AvatarState())
        override val state: StateFlow<AvatarState> = _state
        override val availableAnimations: List<String> = emptyList()
        override fun setEmotion(newEmotion: AvatarEmotion) {
            _state.value = _state.value.copy(emotion = newEmotion)
        }
        override fun playAnimation(animationName: String, loop: Int) {
            _state.value = _state.value.copy(
                currentAnimation = animationName,
                isLooping = loop == 0
            )
        }
    }
}
