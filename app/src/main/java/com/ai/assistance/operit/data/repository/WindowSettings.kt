package com.ai.assistance.operit.data.repository

/**
 * 全屏浮窗内 Avatar 展示区域的可配置尺寸（dp 单位）。
 *
 * 与 [AvatarInstanceSettings]（scale/translate，即 Avatar Transform）**分离存储**：
 * - WindowSettings：描述「浮窗尺寸」——波浪 / 头像 / 点击区的显示直径。
 * - AvatarInstanceSettings：描述数字人本体的缩放与平移（Avatar Transform）。
 *
 * 字段默认值对应 FloatingFullscreenScreen 中原有的硬编码尺寸：
 * - Voice 系列：`isVoiceAvatarEnabled == true` 时使用的尺寸（420 / 320 / 220）。
 * - Plain 系列：无语音头像时的尺寸（300 / 120 / 140）。
 */
data class WindowSettings(
    val waveSizeVoiceDp: Float = 420f,
    val avatarSizeVoiceDp: Float = 320f,
    val tapTargetVoiceDp: Float = 220f,
    val waveSizePlainDp: Float = 300f,
    val avatarSizePlainDp: Float = 120f,
    val tapTargetPlainDp: Float = 140f
) {
    /**
     * 部分更新：仅替换传入的非空字段，其余保留当前值。
     * 调用方需负责对传入值做范围 clamp（见 AvatarControlValidation）。
     */
    fun mergedWith(
        waveSizeVoice: Float? = null,
        avatarSizeVoice: Float? = null,
        tapTargetVoice: Float? = null,
        waveSizePlain: Float? = null,
        avatarSizePlain: Float? = null,
        tapTargetPlain: Float? = null
    ): WindowSettings = WindowSettings(
        waveSizeVoiceDp = waveSizeVoice ?: waveSizeVoiceDp,
        avatarSizeVoiceDp = avatarSizeVoice ?: avatarSizeVoiceDp,
        tapTargetVoiceDp = tapTargetVoice ?: tapTargetVoiceDp,
        waveSizePlainDp = waveSizePlain ?: waveSizePlainDp,
        avatarSizePlainDp = avatarSizePlain ?: avatarSizePlainDp,
        tapTargetPlainDp = tapTargetPlain ?: tapTargetPlainDp
    )
}
