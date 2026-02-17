# Schildi Next 通话与搜索改造计划

- 生成时间: 2026-02-16T20:17:09+08:00
- 工作目录: `/home/kuma/programes/element/element-phone/schildichat-android-next`
- 默认实现假设: 采用 Jitsi Widget/Web 路线（不引入 Jitsi Native SDK）

## 目标

1. Element Call 默认根地址切换为 `https://call.redworker.org`（保留自定义覆盖能力）。
2. 在房间通话入口并入 Jitsi 选项，样式为“Video call using: Element Call / Legacy Call”。
3. 增加房间消息搜索（本地时间线优先，支持消息/图片视频/文件三类结果，点击可定位事件）。

## 原子化执行清单

1. 更新 Element Call 默认地址与 deep link host 白名单。
   - `features/call/impl/.../DefaultCallWidgetProvider.kt`
   - `features/call/impl/.../CallIntentDataParser.kt`
   - `features/call/impl/src/main/AndroidManifest.xml`

2. 扩展消息页面回调链路，支持 Jitsi 呼叫分支。
   - `features/messages/impl/.../MessagesNode.kt`
   - `features/messages/impl/.../threads/ThreadedMessagesNode.kt`
   - `features/messages/impl/.../MessagesFlowNode.kt`

3. 增加通话提供方选择 UI（Element Call / Legacy Call）。
   - `features/messages/impl/.../MessagesView.kt`
   - `features/messages/impl/.../topbars/MessagesViewTopBar.kt`
   - `features/messages/impl/.../topbars/ScMessagesViewTopBarExtensions.kt`

4. 增加房间消息搜索 UI 与本地过滤逻辑。
   - 新增 `features/messages/impl/.../RoomMessageSearchBottomSheet.kt`
   - 在 `MessagesView.kt` 接入入口与跳转定位（`TimelineEvents.FocusOnEvent`）

5. 补充文案资源。
   - `features/messages/impl/src/main/res/values/localazy.xml`

6. 代码优化与一致性检查。
   - 移除重复逻辑，复用 helper。
   - 仅做本次改动范围内优化，不做额外重构。

7. 验证与归档。
   - 对改动文件执行 `lsp_diagnostics`。
   - 运行目标模块编译（至少 `features:messages:impl:compileDebugKotlin` 与 `features:call:impl:compileDebugKotlin`）。
   - 归档计划到 `.zcf/plan/history/`。
