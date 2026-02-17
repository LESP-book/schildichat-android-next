# 修复 SchildiChat Next 自定义 Element Call URL 问题

## 问题描述

当用户在 SchildiChat Next 开发者选项中设置自定义 Element Call URL（如 `https://call.redworker.org`）时，点击开始通话会显示 Element Call 首页而不是直接进入房间通话。

### 根本原因

Matrix Rust SDK 的 `generateWebviewUrl()` 函数在处理自定义 `elementCallUrl` 时，没有正确生成包含 widget 参数的完整 URL。生成的 URL 仅为基础 URL（如 `https://call.redworker.org`），缺少以下必要参数：
- `widgetId`
- `parentUrl`
- `roomId`
- `userId`
- `deviceId`
- `baseUrl`
- `intent`
- 其他配置参数

## 修改方案

### 文件 1：DefaultCallWidgetProvider.kt

**路径**：`features/call/impl/src/main/kotlin/io/element/android/features/call/impl/utils/DefaultCallWidgetProvider.kt`

#### 修改内容

将整个文件替换为以下内容：

```kotlin
/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.utils

import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.isDm
import io.element.android.libraries.matrix.api.widget.CallWidgetSettingsProvider
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.services.appnavstate.api.ActiveRoomsHolder
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

private const val EMBEDDED_CALL_WIDGET_BASE_URL = "https://appassets.androidplatform.net/element-call/index.html"

@ContributesBinding(AppScope::class)
class DefaultCallWidgetProvider(
    private val matrixClientsProvider: MatrixClientProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val callWidgetSettingsProvider: CallWidgetSettingsProvider,
    private val activeRoomsHolder: ActiveRoomsHolder,
) : CallWidgetProvider {
    override suspend fun getWidget(
        sessionId: SessionId,
        roomId: RoomId,
        clientId: String,
        languageTag: String?,
        theme: String?,
    ): Result<CallWidgetProvider.GetWidgetResult> = runCatchingExceptions {
        val matrixClient = matrixClientsProvider.getOrRestore(sessionId).getOrThrow()
        val room = activeRoomsHolder.getActiveRoomMatching(sessionId, roomId)
            ?: matrixClient.getJoinedRoom(roomId)
            ?: error("Room not found")

        val customBaseUrl = appPreferencesStore.getCustomElementCallBaseUrlFlow().firstOrNull()
        val baseUrl = customBaseUrl ?: EMBEDDED_CALL_WIDGET_BASE_URL

        val roomInfo = room.info()
        val isEncrypted = roomInfo.isEncrypted ?: room.getUpdatedIsEncrypted().getOrThrow()
        val isDirect = room.isDm()
        val hasActiveCall = roomInfo.hasRoomCall

        val widgetSettings = callWidgetSettingsProvider.provide(
            baseUrl = baseUrl,
            encrypted = isEncrypted,
            direct = isDirect,
            hasActiveCall = hasActiveCall,
        )

        var callUrl = room.generateWidgetWebViewUrl(
            widgetSettings = widgetSettings,
            clientId = clientId,
            languageTag = languageTag,
            theme = theme,
        ).getOrThrow()

        // 修复：检查自定义 URL 是否缺少必要的 widget 参数
        // 如果 Rust SDK 未正确处理自定义 URL，手动构建完整 URL
        if (customBaseUrl != null && !callUrl.contains("widgetId=")) {
            Timber.w("Custom Element Call URL missing widget parameters, manually building URL")

            // 确定 intent 类型
            val intent = when {
                isDirect && hasActiveCall -> "join_existing_dm"
                hasActiveCall -> "join_existing"
                isDirect -> "start_call_dm"
                else -> "start_call"
            }

            // 构建完整的 widget URL
            val uriBuilder = Uri.parse(customBaseUrl).buildUpon()

            // 添加 /room 路径（如果 baseUrl 不包含的话）
            val baseUrlPath = Uri.parse(customBaseUrl).path ?: ""
            if (!baseUrlPath.contains("room")) {
                uriBuilder.appendPath("room")
            }

            // 添加必要的 widget 参数
            uriBuilder
                .appendQueryParameter("widgetId", widgetSettings.id)
                .appendQueryParameter("parentUrl", "element://call")
                .appendQueryParameter("roomId", roomId.value)
                .appendQueryParameter("userId", sessionId.value)
                .appendQueryParameter("deviceId", matrixClient.deviceId() ?: "")
                .appendQueryParameter("baseUrl", matrixClient.homeserverUrl())
                .appendQueryParameter("enableE2EE", if (isEncrypted) "true" else "false")
                .appendQueryParameter("perParticipantE2EE", if (isEncrypted) "true" else "false")
                .appendQueryParameter("confineToRoom", "true")
                .appendQueryParameter("appPrompt", "false")
                .appendQueryParameter("skipLobby", "true")
                .appendQueryParameter("intent", intent)

            // 添加可选参数
            languageTag?.let { uriBuilder.appendQueryParameter("lang", it) }
            theme?.let { uriBuilder.appendQueryParameter("theme", it) }

            callUrl = uriBuilder.build().toString()
            Timber.d("Built custom Element Call URL: $callUrl")
        }

        val driver = room.getWidgetDriver(widgetSettings).getOrThrow()

        CallWidgetProvider.GetWidgetResult(
            driver = driver,
            url = callUrl,
        )
    }
}
```

### 文件 2：Element Call 配置（可选）

**路径**：Element Call 服务端的 `public/config.json`

添加 `app_prompt: false` 以禁用"在浏览器中继续"/"在应用中打开"弹窗：

```json
{
  "default_server_config": {
    "m.homeserver": {
      "base_url": "https://redchat.uk",
      "server_name": "redchat.uk"
    }
  },
  "livekit": {
    "livekit_service_url": "https://matrixrtc.redchat.uk"
  },
  "app_prompt": false,
  "features": {
    "feature_use_device_session_member_events": true
  },
  "ssla": "https://static.element.io/legal/element-software-and-services-license-agreement-uk-1.pdf",
  "matrix_rtc_session": {
    "wait_for_key_rotation_ms": 3000,
    "membership_event_expiry_ms": 180000000,
    "delayed_leave_event_delay_ms": 18000,
    "delayed_leave_event_restart_ms": 4000,
    "network_error_retry_ms": 100
  }
}
```

## 执行步骤

### 步骤 1：备份原文件

```bash
cp /home/kuma/programes/element/schildichat-android-next/features/call/impl/src/main/kotlin/io/element/android/features/call/impl/utils/DefaultCallWidgetProvider.kt \
   /home/kuma/programes/element/schildichat-android-next/features/call/impl/src/main/kotlin/io/element/android/features/call/impl/utils/DefaultCallWidgetProvider.kt.backup
```

### 步骤 2：应用修改

使用 Edit 工具修改 `DefaultCallWidgetProvider.kt` 文件，按照上述"修改内容"部分进行替换。

### 步骤 3：验证编译

```bash
cd /home/kuma/programes/element/schildichat-android-next
./gradlew :features:call:impl:compileDebugKotlin
```

### 步骤 4：构建 APK

```bash
# Debug 版本（推荐用于测试）
./gradlew assembleDebug

# 或 Release 版本
./gradlew assembleRelease
```

### 步骤 5：安装测试

```bash
# Debug APK 位置
ls -la app/build/outputs/apk/debug/

# 使用 adb 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 验证方法

1. 打开 SchildiChat Next
2. 进入开发者选项，设置自定义 Element Call URL 为 `https://call.redworker.org`
3. 进入任意房间，点击开始通话
4. **预期结果**：直接进入通话界面，不再显示 Element Call 首页

## 注意事项

1. **API 兼容性**：此修改依赖 `matrixClient.deviceId()` 和 `matrixClient.homeserverUrl()` 方法，需确认这些方法在当前版本中可用
2. **如果编译失败**：可能需要调整方法名称以匹配实际 API，使用 Grep 工具搜索正确的方法名
3. **回滚方法**：如果修改导致问题，可以从 `.backup` 文件恢复原始代码

## 相关文件

- `features/call/impl/src/main/kotlin/io/element/android/features/call/impl/utils/DefaultCallWidgetProvider.kt` - 主要修改文件
- `features/call/impl/src/main/kotlin/io/element/android/features/call/impl/utils/CallWidgetProvider.kt` - 接口定义
- `libraries/matrix/impl/src/main/kotlin/io/element/android/libraries/matrix/impl/widget/MatrixWidgetSettings.kt` - Widget URL 生成
- `libraries/matrix/api/src/main/kotlin/io/element/android/libraries/matrix/api/widget/CallWidgetSettingsProvider.kt` - 设置提供者接口
