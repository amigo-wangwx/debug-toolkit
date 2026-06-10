# AGENTS.md

## 交流规则

- 默认中文交流。
- 修改前必须先读现有代码，明确问题判断、拟修改文件、diff 草案和风险点。
- 未经明确授权不得修改文件；授权词包括“可以修改”“确认修改”“开始修改”“应用这个方案”“执行 patch”。
- 修改时保持最小范围，不做无关重构，不覆盖调用方业务工程改动。

## 项目目标

本项目维护 DebugToolkit、网络拦截库和 ASM 注入插件的独立源码模板，用于同步到 Android 多 App 工程中。

核心目录：

- `libs/debug-tool/debug-toolkit`：浮窗、调试开关、网络配置入口、日志查看等调试工具 UI。
- `libs/debug-tool/network-interceptor`：网络拦截底层库，包含 JSON 配置文件、rule 选择、URL 重写、独立配置编辑器。
- `build-logic/debug-network-interceptor-plugin`：Gradle/ASM 插件，自动注入 OkHttp Builder 和非主进程 Application guard。
- `scripts/tools/generate_debug_toolkit.py`：生成脚本，是同步到业务工程的单一模板来源。

## 重要约束

- `generate_debug_toolkit.py` 必须和三个源码目录保持完全一致。
- 修改任意运行时代码、Manifest、资源、插件代码后，必须同步更新生成脚本。
- 生成脚本运行后，生成结果不能与模板源码出现差异。
- 不允许只改生成结果、不改脚本；也不允许只改脚本、不验证生成结果。
- 网络拦截配置文件使用 JSON 格式：`debug_network_config.json`。
- 网络映射选择使用 `selectRuleIds`，不是 `selectMode`、不是 `groups`。
- 一个 rule 表示一组可独立应用的 mappings；可以选择一个 rule，也可以同时选择多个 rule。
- 多个 rule 命中同一个 source 时，`selectRuleIds` 中靠后的 rule 覆盖靠前的 rule。
- 配置编辑器运行在独立进程，调用方不应手动适配 Application；由 ASM 注入 process guard。

## 日志要求

- 控制链路日志和请求流量日志要分开。
- 即使关闭网络详细日志，也应保留配置加载、rule 选择、映射应用、拦截器安装、ASM 注入等控制日志。
- 请求命中、请求/响应体等流量日志受 `flowLogEnabled` 控制。
- 编辑器必须记录 URI、读写成功/失败、文本长度、广播刷新结果，方便定位“打开无内容/闪退”。
- 面板必须记录打开、重载、编辑、应用、重启、恢复模板和 rule 勾选结果。

## UI 与链路约束

- 网络映射 UI 放在 `network-interceptor` 底层库中，`debug-toolkit` 浮窗只负责唤起入口。
- 不再使用 spinner 承载单选/多选映射选择；映射选择以 rule 勾选为准。
- 浮窗 UI 修改要同时检查 XML、Adapter 和 Service 点击区域，避免变形或无法点击。
- JSON 文件管理器唤起的配置编辑器必须支持内容读取、编辑、格式化、保存，并运行在独立进程。
- 非主进程跳过宿主 Application.onCreate 的逻辑由 ASM 注入，调用方不需要手动修改 Application。

## 生成脚本同步流程

修改任意源码目录后，必须把最新内容同步进 `scripts/tools/generate_debug_toolkit.py` 的 `GENERATED_FILES`。

建议顺序：

1. 修改 `libs/debug-tool/debug-toolkit`、`libs/debug-tool/network-interceptor` 或 `build-logic/debug-network-interceptor-plugin`。
2. 将这三个目录的最新文件内容同步到 `generate_debug_toolkit.py`。
3. 运行生成脚本，确认生成后的文件没有回退。
4. 比对脚本内嵌模板和实际文件，确保逐字一致。

## 验证清单

修改后至少执行：

```bash
python3 -m py_compile scripts/tools/generate_debug_toolkit.py
python3 scripts/tools/generate_debug_toolkit.py
git diff --check
```

并做脚本模板和生成文件一致性校验：

- `GENERATED_FILES` 数量和实际文件数量一致。
- 无 missing。
- 无 extra。
- 无 content diff。

同步回 FunShorts 等业务工程后，还需要在业务工程执行：

```bash
./gradlew :apps:FunShorts:assembleGoogleFunshortsDebug --console=plain
```

## 风险提示

- 不要在未确认的情况下从业务工程物理删除 debug-tool 相关目录。
- 如果需要从业务工程迁移到本项目，优先复制并保留业务工程可构建状态。
- 如果确实要物理移动，必须先说明会导致业务工程缺模块，并取得明确确认。
- `/Users/wangwx/github/debug-toolkit` 是独立维护目录；向业务工程同步时，要再次校验生成脚本和生成代码的一致性。
