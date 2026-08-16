# Operit Avatar 外部控制系统 后续开发规划书

> 状态：Phase 1/2/3 已完成（Phase 3 待 CI 构建 + 真机验证），Phase 4 起待实施
> 依据：`~/.config/operit.conf`（OPERIT_BASE / OPERIT_TOKEN），已实现的 Avatar HTTP Control API v1（提交 `478c75e`）
> 关联文档：`docs/codex-控制-operit.md`

## 一、项目定位

### 1.1 项目名称
Operit Avatar Control

### 1.2 当前状态（第一阶段已完成）
Avatar HTTP Control API v1 已具备：

- Avatar 状态查询
- Emotion 控制
- Animation 控制
- Settings 控制
- Bearer Token 鉴权
- 当前 AvatarController 生命周期桥接
- 参数校验
- 持久化 + 运行时更新
- 单元测试（20/20）
- Android CI Build / UnitTest 全绿

当前架构已形成：

```
外部客户端
    ↓
HTTP API (/api/web/avatar/*)
    ↓
WebChatAvatarBridge
    ↓
AvatarControlManager
    ↓
当前 AvatarController
    ↓
Avatar UI
```

**约束**：后续开发不得重新建立第二套 Avatar Controller、第二套 HTTP Server 或第二套 Avatar 状态系统。

## 二、总体目标

```
                  ┌──────────────┐
                  │   AI Agent   │
                  └──────┬───────┘
                         │
                  ┌──────▼───────┐
                  │   AI Tools   │
                  └──────┬───────┘
                         │
                  ┌──────▼───────┐
                  │     CLI      │
                  └──────┬───────┘
                         │
                         ▼
              ┌─────────────────────┐
              │ Avatar HTTP API     │
              │ Bearer Auth         │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │ Avatar Control      │
              │ Layer               │
              ├─────────────────────┤
              │ State               │
              │ Emotion             │
              │ Animation           │
              │ Settings            │
              │ Window              │
              │ Idle                │
              │ Wave                │
              │ IK                  │
              └──────────┬──────────┘
                         │
                         ▼
              当前 AvatarController
                         │
                         ▼
                   Avatar Runtime
```

核心原则：**AI/CLI 只提出控制意图，Avatar Control Layer 负责验证和执行，渲染层负责最终表现。**

## 三、开发原则

### 3.1 控制权不能下沉到 AI
AI 不允许直接操作：DragonBones / View / Compose UI / AvatarState / Repository 内部对象 / Renderer。
AI 只能调用高层 Tool，例如 `avatar_set_emotion("happy")`，而不能 `dragonbones.play(...)`、`view.scale(...)`。

### 3.2 不增加第二套 Controller
全局始终保持：当前 UI Avatar → AvatarController → AvatarControlManager。HTTP / CLI / AI Tool 只是不同入口。

### 3.3 API 优先
新能力优先进入 Avatar Control Layer，再决定暴露 HTTP / CLI / AI Tool；不允许各入口各自实现一套。

## 四、Phase 1：CLI

- 目标：建立 Avatar HTTP API 的命令行客户端。
- 命令：`operit-avatar state` / `operit-avatar emotion happy` / `operit-avatar animation blink [--loop]` / `operit-avatar settings --scale 1.5 --x 100 --y -50`
- 架构：CLI → HTTP Client → Bearer Token → /api/web/avatar/*。CLI 不直接进入 Android Avatar 内部。
- 验收：state 能返回当前 Avatar 状态；HTTP 错误转换；JSON / 人类可读双输出；测试；README。

## 五、Phase 2：AI Tool

- 目标：让 AI Agent 直接控制 Avatar。
- Tool：`avatar_get_state` / `avatar_set_emotion` / `avatar_play_animation` / `avatar_update_settings`。
- 架构：AI → Tool → HTTP Client → Avatar HTTP API（禁止 AI → Kotlin AvatarController）。
- 安全边界：Tool 校验参数、限制可用 emotion/animation、限制 settings 范围、处理 Avatar 未就绪、不允许异常杀死 Tool Runtime。

- 交付（2026-08-16）：Operit ToolPkg `avatar_control`，位置 `~/operit-tools/avatar-toolpkg/`（含 manifest.json / main.js / packages/avatar_control.js / README.md / tests/run_tests.js）。
  - 工具通过 `toolCall('http_request')` 调用 `/api/web/avatar/*`，Bearer 鉴权；环境变量 `AVATAR_BASE_URL`（默认 `http://127.0.0.1:8094`）/ `AVATAR_TOKEN`（必填）。
  - 安全边界已落地：情绪本地枚举校验、scale 0.1–5.0 / translate ±2000 客户端钳制、token 缺失保护、异常 try/catch 兜底、503/400/401 透传 error token。
  - 测试：Node harness（mock 宿主 + 真实 HTTP 到模拟 server），62 断言全绿。
  - 部署：内置包 `assets/packages/` / 外部包 `Android/data/<applicationId>/files/packages/` / Debug 广播安装。
  - **端到端验证（2026-08-16，官方 v1.12.1 实测）**：官方版自带完整 `/api/web/avatar/*`，无需 fork 构建。
    - 读链路：Operit AI 用 `extended_http_tools:http_request` → `GET /api/web/avatar/state` → 200 真实状态 ✅
    - 写链路：AI 三步全 200 —— GET state（IDLE）→ POST emotion=SAD → GET 确认 SAD ✅（operit-avatar 独立复核一致）
    - 约束：写操作须 `ready: true`（仅 FULLSCREEN 浮窗或助手配置页渲染时注册 AvatarController；WINDOW/BALL 浮窗不注册 → 503 avatar_not_ready）
    - 结论：AI → HTTP → AvatarControlManager → 当前 AvatarController 全链路打通；`avatar_control` 包的价值 = token 不进 AI 上下文（更安全），及参数本地校验/钳制
## 六、Phase 3：浮窗尺寸配置化 ✅（已实施，待 CI/真机验证）

- 目标：FloatingFullscreenScreen 中硬编码尺寸（420/320/220 与 300/120/140）→ 可配置。
- 架构：WindowSettings（data class）→ AvatarRepository（StateFlow + SharedPreferences）→ FloatingFullscreenScreen（collectAsState）。
- 实现（6 个 dp 字段，非 width/height）：
  - Voice 系列（语音头像模式）：`waveSizeVoiceDp=420 / avatarSizeVoiceDp=320 / tapTargetVoiceDp=220`
  - Plain 系列（无语音头像模式）：`waveSizePlainDp=300 / avatarSizePlainDp=120 / tapTargetPlainDp=140`
- 新增文件：`data/repository/WindowSettings.kt`（含 `mergedWith` 部分更新，data class 不可变）。
- Repository：`_windowSettings` StateFlow + `updateWindowSettings/getWindowSettings` + SharedPreferences 持久化（KEY `avatar_window_settings`，init 加载）。
- 校验：`AvatarControlValidation` 新增 `clampWaveSize(100-2000)/clampAvatarSize(40-1000)/clampTapTarget(40-1000)`。
- API：`GET/POST /api/web/avatar/window`（WebAvatarWindowRequest 6 可空字段部分更新 + clamp；响应 6 字段全量）。
  - GET 返回当前全量配置；POST 仅更新传入的非空字段（partial update）。
- 测试：WindowSettingsTest（5）+ AvatarControlValidationTest 新增 6 个 clamp 测试。
- 注意：Window Size 与 Avatar Transform（scale/translate，AvatarInstanceSettings）**分离存储**，不可混为一谈。

## 七、Phase 4：Random Idle

- 把随机 Idle 从隐式行为变成可控能力。
- 新增 `AvatarIdleController`：Idle 开关 / 间隔 / 随机动画选择 / 冲突处理 / 当前状态判断。
- API：`GET/POST /api/web/avatar/idle`（`{enabled, intervalMs}`）。
- 优先级：用户/AI 主动控制 → Emotion → Animation → Idle（Idle 最低，不抢占 THINKING/LISTENING/HAPPY/SAD）。

## 八、Phase 5：Wave 控制

- 先只读审计 WaveVisualizer（API / 生命周期 / 线程模型 / 状态来源）→ 再建 `AvatarWaveController`。
- 第一版 API：start / stop / setAmplitude / setFrequency（`{enabled, amplitude, frequency}`）。
- 原则：不能为了 HTTP API 重写 WaveVisualizer。

## 九、Phase 6：IK

- IK 最后做（最易侵入渲染层）。
- 第一阶段只读审计：DragonBones 版本 / Armature / Bone / IK constraint / 是否实际使用 IK / 渲染线程 / UI 线程限制 / 生命周期。**禁止修改代码**。
- 第二阶段（若底层支持）：`AvatarIKController`，高层接口 `{target:"head", x, y, enabled}`。
- 绝对禁止 HTTP API 暴露 Bone / Armature / Constraint / DragonBones Object。

## 十、最终 API 规划

| 路径 | 方法 | 说明 |
|---|---|---|
| `/api/web/avatar/state` | GET | Avatar 状态（Phase 1 已完成） |
| `/api/web/avatar/emotion` | POST | 设置情绪（Phase 1 已完成） |
| `/api/web/avatar/animation` | POST | 播放动画（Phase 1 已完成） |
| `/api/web/avatar/settings` | POST | 实例设置 scale/translate（Phase 1 已完成） |
| `/api/web/avatar/window` | GET/POST | 浮窗尺寸（Phase 3） |
| `/api/web/avatar/idle` | GET/POST | 随机 Idle 控制（Phase 4） |
| `/api/web/avatar/wave` | GET/POST | 波浪控制（Phase 5） |
| `/api/web/avatar/ik` | GET/POST | IK 控制（Phase 6，需审计确认） |

---

## 附录 A：与现有架构核对结论（2026-08-16 只读核对）

- HTTP 路由：`WebChatHttpBridge.kt` 中 `avatarBridge`（行 103），四个 handler：`handleAvatarState/Emotion/Animation/Settings`（行 999/1008/1017/1026）；`avatarResponse`（行 1035）。路径前缀 `/api/web/avatar/*`，统一走 `requireBearerToken`。
- 桥接层：`integrations/http/bridge/WebChatAvatarBridge.kt`（`resolveState`/`setEmotion`/`playAnimation`/`updateSettings`）。
- 控制层：`AvatarControlManager`（单例桥，`getActiveController()` 返回当前 UI 注册的 `AvatarController`；不新建 Controller）。
- 状态：`AvatarState(emotion, currentAnimation, isLooping, playbackNonce)` 纯内存；`AvatarEmotion` = IDLE/LISTENING/THINKING/HAPPY/SAD/CONFUSED/SURPRISED。
- 持久化：`AvatarRepository.updateAvatarSettings` + `AvatarInstanceSettings(scale, translateX, translateY)`；scale clamp 0.1–5.0，translate clamp ±2000。
- API 响应字段：`avatarId / emotion / animation / isLooping / scale / translateX / translateY / ready`。
- 未就绪：返回 `{"success":false,"error":"avatar_not_ready"}`（HTTP 503）；非法参数 4xx。
- 安全：监听地址 `0.0.0.0:8094`（既有事实），`/api/web/*` 全部 Bearer。

## 附录 B：实施顺序建议

1. Phase 1 CLI —— 本地 Termux 即可开发测试，**不改 Android 源码**（纯 HTTP 客户端）。
2. Phase 2 AI Tool —— 同样不改 Android 源码。
3. Phase 3 浮窗尺寸 —— 需改 Android 端（FloatingFullscreenScreen）+ 走 GitHub Actions。
4. Phase 4 Random Idle —— 需改 Android 端。
5. Phase 5 Wave —— 先只读审计，再定方案。
6. Phase 6 IK —— 先只读审计（禁止改代码），确认支持再设计。

## 附录 C：验收状态追踪

- [x] Phase 1 基础命令（state/emotion/animation/settings）
- [x] Phase 1 HTTP Client + Token 配置
- [x] Phase 1 双输出模式（--json / 人类可读）
- [x] Phase 1 HTTP 错误转换（401/400/503/网络）
- [x] Phase 1 CLI 测试（mock server）
- [x] Phase 1 README
- [x] Phase 2 AI Tool（4 个 tool，ToolPkg avatar_control）
- [x] Phase 3 浮窗尺寸配置化（编码完成，待 CI 构建 + 真机验证 `/api/web/avatar/window`）
- [ ] Phase 4 Random Idle
- [ ] Phase 5 Wave
- [ ] Phase 6 IK
