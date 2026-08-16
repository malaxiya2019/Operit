# codex 控制 Operit — 操作手册

通过 Operit 官方「外部 HTTP 调用」服务，让 Termux 里的 codex 直接驱动手机上的 Operit。

## 前置

1. Operit → 设置 → 数据和权限 → **外部 HTTP 调用** → 开启（默认端口 8094）
2. 记录页面的 **Bearer Token**
3. 写入 `~/.config/operit.conf`：
   ```bash
   echo "OPERIT_BASE=http://127.0.0.1:8094" > ~/.config/operit.conf
   echo "OPERIT_TOKEN=<你的token>" >> ~/.config/operit.conf
   ```

## CLI：`~/bin/operit`

三大通道：
- **HTTP Chat**：`POST /api/external-chat`（同步/SSE），最常用
- **A2A 1.0**：`POST /a2a`（JSON-RPC，agent 间标准协议）
- **管理 API**：`/api/web/*`（模型/角色/对话/消息/记忆/设置/上传）— 与 App 内设置同源，全部可远程控制

### 对话

| 命令 | 用途 |
|---|---|
| `operit health` | 检查服务 + 鉴权 |
| `operit chat "任务"` | **HTTP Chat 同步调用**（最常用，阻塞拿 `ai_response`） |
| `operit stream "任务"` | SSE 流式输出 |
| `operit chat --new --group 分组 "任务"` | 新建对话发消息 |
| `operit chat --chat-id <id> "任务"` | 指定对话续聊 |
| `operit chat --float "任务"` | 启动悬浮窗（`--mode WINDOW|BALL|FULLSCREEN|RESULT_DISPLAY|SCREEN_OCR`） |

### A2A

| 命令 | 用途 |
|---|---|
| `operit a2a "任务"` | SendMessage（阻塞轮询到终态，取 artifacts） |
| `operit a2a-stream "任务"` | SendStreamingMessage（SSE） |
| `operit a2a-list` / `a2a-task <id>` / `a2a-cancel <id>` | 任务管理 |
| `operit card` | A2A agent-card（能力声明） |

### 管理 API（控制 App 状态）

| 命令 | 用途 |
|---|---|
| `operit models` | 列出模型配置与当前选择 |
| `operit model-switch <config_id> [--index N] [--force]` | 切模型（角色卡锁定时需 `--force`） |
| `operit characters` | 列出角色卡 / 角色组 |
| `operit character-switch <card\|group> <id>` | 切换当前角色 |
| `operit memory` / `operit memory-switch <profile_id>` | 查看 / 切换记忆空间 |
| `operit chats` | 列出全部对话 |
| `operit chat-create [--title T] [--group G] [--card NAME] [--group-id GID] [--no-current]` | 建对话 |
| `operit chat-select <chat_id>` | 切换当前对话 |
| `operit chat-rename <chat_id> <新标题>` | 改标题 |
| `operit chat-update <chat_id> [--title] [--group] [--card] [--lock\|--unlock] [--pin\|--unpin]` | 更新对话（置顶/锁定/改绑定） |
| `operit chat-delete <chat_id>` | 删除对话（锁定对话会被拒绝） |
| `operit send <chat_id> "消息" [--attachment <id>]` | 向指定对话发消息（SSE 实时打印回复） |
| `operit messages <chat_id> [--limit N]` | 读取该对话最近消息 |
| `operit settings` | 读取输入设置（thinking/tools/权限等） |
| `operit settings-set key=value ...` | 更新输入设置（如 `enable_thinking_mode=true`） |
| `operit upload <file>` | 上传文件，返回 `attachment_id`（供 send 用） |

## codex 调用约定

- 交任务：`operit chat "..."`（等 JSON 里 `ai_response`）
- 要流式：`operit stream "..."`
- 标准 agent 协议：`operit a2a "..."`
- 指定对话控制：先 `operit chats` 拿 `chat_id`，再 `operit send <id> "..."`
- 控制模型/角色/设置：`operit model-switch / character-switch / settings-set`

### 命令终端控制（super_admin 工具包，实测可用）

Operit 无直接终端 REST 端点，通过 external-chat 让 AI 用工具代理执行命令：

```bash
# 让 AI 在 Operit 终端（Ubuntu + root）执行命令并报告输出
operit chat --tool-status "用 super_admin:terminal 执行：<命令>，报告输出"

# 同一对话续聊，保持终端会话连贯（session 复用，上下文不丢）
operit chat --chat-id <id> --tool-status "继续在同一会话执行：<命令>"
```

可用工具：`terminal`（执行拿输出，`background=true` 后台跑）、`terminal_wait`（等命令完成）、`terminal_input`（写交互输入：input+control=enter/ctrl+c/tab/esc）、`terminal_getscreen`（抓屏幕）、`shell`（Shizuku/Root 跑 Android 系统命令，本机无 root 不可用）。
终端身份：Ubuntu 容器 + root，挂载 sdcard/storage。
注意：AI 是代理层，偶尔需明示"用 super_admin:terminal 工具"。

## 注意事项

1. **deepsearching 劫持**：`ai_planning`（深度搜索）开启时，external-chat 消息会被 `deepsearching` 插件拦截走计划生成 → 报「❌ 生成计划失败」。该插件每次执行后自动关闭 toggle，**重发一条即恢复**。
2. **模型 key**：Operit 配置的模型 API key 失效会 401，需在 Operit 设置里置换。
3. **A2A 响应结构**：GetTask/ListTasks 返回的 task 直接是 `result`（无 `.task` 嵌套），SendMessage 返回 `result.task`。CLI 已兼容。
4. **chat-update 响应滞后**：PATCH 返回更新前的 snapshot（滞后一拍），但变更已生效，以 `operit chats` 复查为准。
5. **device 边界**：本通道是 HTTP 管理面，不含设备 UI 自动化、系统级权限（root/adb 的 Intent 广播不可用，本机无 root）。

## 通道对比

| 通道 | 依赖 | 状态 |
|---|---|---|
| HTTP Chat API (`:8094`) | 无（回环即可） | ✅ 服务在线 |
| A2A 1.0 (`/a2a`) | 无 | ✅ 服务在线 |
| 管理 API (`/api/web/*`) | 无（同 Bearer token） | ✅ 全读写可用 |
| Intent 广播 | adb/root | ⚠️ 本机无 root、adb 未连 |

## Operit 工作流可控制 ✅（2026-08-15 全链路实测）

工作流系统（trigger/execute/condition/logic/extract 五类节点）可通过 external-chat 让 AI 用 workflow 工具包完全控制。**无直接 REST 端点，靠 AI 代理调用。**

### 工作流工具（workflow 包，AI 经 use_package + package_proxy 调用）
- `get_all_workflows`：列出全部（概要：ID/名称/启用/节点数/执行统计）
- `get_workflow`：完整详情（nodes + connections），参数 `workflow_id`
- `create_workflow`：创建，`name` 必填；`nodes`/`connections` 为 JSON 数组字符串（封装层可传对象数组自动 stringify）；`enabled` 默认 true
- `update_workflow`：整体覆盖更新（nodes/connections 整体替换）
- `patch_workflow`：增量更新（node_patches / connection_patches，op: add|update|remove）
- `enable_workflow` / `disable_workflow`：启停
- `trigger_workflow`：触发执行（= UI 手动触发）
- `delete_workflow`：删除

### 节点/连线语义（实测要点）
- 触发类型：`manual` / `schedule`（interval/specific_time/cron）/ `tasker` / `intent`（系统广播）/ `speech`（语音命中正则）
- Execute 节点：`actionType` = 任意工具名（如 `time:get_time`、`http_request`、`visit_web`），`actionConfig` 支持参数引用（{nodeId}）
- 连线 condition：`on_success`/`on_error`；对 Condition/Logic 节点留空= true 分支、`false`= false 分支、其它字符串当 Regex
- 节点 id 可省略（服务端生成），但要建 connections 建议显式写 id

### 实测证据（2026-08-15，测试工作流已删除）
- 创建 trigger(manual) + execute(time:get_time) + 连线 on_success → 成功，ID 返回
- `trigger_workflow` 触发 → **真实执行**：`lastExecutionStatus=SUCCESS`、`totalExecutions=1`、`successfulExecutions=1`
- `disable_workflow` → enabled=false；`enable_workflow` → enabled=true；get_workflow 复核一致
- `delete_workflow` 删除 → get_all_workflows 归零

### 用法
```bash
operit chat --chat-id <id> --tool-status "用 create_workflow 创建测试工作流：<name>，节点 trigger+execute，连线 on_success"
operit chat --chat-id <id> --tool-status "用 trigger_workflow 触发 <workflow_id>，报告执行结果"
```

## Operit 助手配置可控制 ✅（2026-08-15 调查实测）

Operit 的「助手配置」分三个层面，前两层可控、第三层不可控：

### 1. 输入/运行设置 — HTTP 直通可控 ✅（operit settings/settings-set，已实测）
`/api/web/input-settings`（GET+PATCH）字段：enable_thinking_mode、thinking_quality_level、enable_memory_auto_update、enable_auto_read、enable_max_context_mode、enable_tools、disable_stream_output、disable_user_preference_description、permission_level（ALLOW/…）。上下文长度（base/max/active context_length_k）只读。
实测：enable_auto_read false→true→false 写成功。

### 2. 平台级配置 — AI 代理可控 ✅（operit_editor 包，2026-08-15 只读实测）
`use_package operit_editor` → `package_proxy` 调用，工具全清单：
- 模型配置：`list_model_configs` / `create_model_config` / `update_model_config` / `delete_model_config`（默认配置不可删）/ `test_model_config_connection`
- 功能绑定（11 个功能：CHAT/SUMMARY/TRANSLATION/MEMORY/IMAGE_RECOGNITION/AUDIO_RECOGNITION/VIDEO_RECOGNITION/GREP/ROLE_RESPONSE_PLANNER/TITLE_GENERATION/UI_CONTROLLER）：`list_function_model_configs` / `get_function_model_config` / `set_function_model_config`
- 上下文总结：`get_context_summary_config` / `set_context_summary_config`
- 角色卡（人格设定）：`list_character_cards` / `get_character_card` / `create|update|delete_character_card` / `set_active_character_card` / `import|export_character_card_from|to_tavern_json`
- 语音：`get_speech_services_config` / `set_speech_services_config` / `test_tts_playback`（TTS 引擎 SIMPLE/HTTP/OPENAI_WS/SILICONFLOW/MINIMAX/MIMO/DOUBAO/OPENAI/VITS；STT SHERPA_NCNN/OPENAI/DEEPGRAM）
- 沙盒包/Skill/MCP/环境变量：`list_sandbox_packages` / `set_sandbox_package_enabled` / `read_environment_variable` / `write_environment_variable` / `debug_install_js_package` / `debug_install_toolpkg` / `ping_mcp`；MCP 配置目录 /sdcard/Download/Operit/mcp_plugins/（Linux 运行侧 ~/mcp_plugins/，npx 实际走 pnpm dlx）

### 3. 纯 UI 层不可控 ❌
- **Avatar 数字人形象**（ui/features/assistant/，App 内「助手配置」页：头像/表情动画/DragonBones 模型等）无任何 /api/web/* 端点，纯本地 AvatarRepository 存储 → HTTP 管不到。
- 纯 UI 布局偏好不可控。

### 实测快照（2026-08-15）
- 模型：default / DEEPSEEK / deepseek-v4-flash / sk-***41，maxTokens=4096(未启用)，enableToolCall=true，enableDirectImageProcessing=true，contextLength=64 / max=200 / summaryThreshold=0.7 / 消息数16
- 功能绑定：11 个功能全绑 default，index 0
- TTS=SIMPLE_TTS，STT=SHERPA_NCNN（sttHttpConfig 默认 openai whisper-1 但无 key）
- 角色卡：default_character "Operit"（FOLLOW_GLOBAL，工具白名单关闭）

### 用法
```bash
# HTTP 直通（输入设置）
operit settings; operit settings-set enable_thinking_mode=true

# AI 代理（平台配置只读/写）
operit chat --tool-status "用 operit_editor:list_model_configs 列出模型配置"
operit chat --tool-status "用 operit_editor:update_model_config 把 default 的 maxTokens 设为 8192"
```

# Avatar 数字人外部控制 ✅（2026-08-16）

## 通道
- CLI：`~/bin/operit-avatar`（state / emotion / animation / settings / window / config / health）
- HTTP API：`/api/web/avatar/*`（Bearer 鉴权，监听 8094）
- AI 代理：`extended_http_tools:http_request` 直调，或 Phase 2 ToolPkg `avatar_control`（use_package + package_proxy）

## 端点
| 路径 | 方法 | 说明 |
|---|---|---|
| `/api/web/avatar/state` | GET | Avatar 状态（emotion/animation/scale/ready） |
| `/api/web/avatar/emotion` | POST | 设置情绪（IDLE/LISTENING/THINKING/HAPPY/SAD/CONFUSED/SURPRISED） |
| `/api/web/avatar/animation` | POST | 播放动画（name + loop） |
| `/api/web/avatar/settings` | POST | 实例设置 scale(0.1-5.0) / translate(±2000) |
| `/api/web/avatar/window` | GET/POST | 浮窗尺寸（waveSizeVoice=420 / avatarSizeVoice=320 / tapTargetVoice=220 / waveSizePlain=300 / avatarSizePlain=120 / tapTargetPlain=140，dp，部分更新+clamp） |

## 约束
- 写操作（emotion/animation/settings）必须浮窗/助手配置页渲染中（`ready: true`），否则 503 `avatar_not_ready`；读操作（state/window）无此限制。
- Window Size（浮窗尺寸）与 Avatar Transform（scale/translate）分离存储，互不影响。
- 端到端实测：AI 经 HTTP 工具三步（GET→POST emotion SAD→GET 复核）真实驱动数字人表情。
