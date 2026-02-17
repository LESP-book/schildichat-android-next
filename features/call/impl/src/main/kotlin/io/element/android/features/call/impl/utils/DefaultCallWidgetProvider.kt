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
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.services.appnavstate.api.ActiveRoomsHolder
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

private const val EMBEDDED_CALL_WIDGET_BASE_URL = "https://call.redworker.org"

@ContributesBinding(AppScope::class)
class DefaultCallWidgetProvider(
    private val matrixClientsProvider: MatrixClientProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val callWidgetSettingsProvider: CallWidgetSettingsProvider,
    private val activeRoomsHolder: ActiveRoomsHolder,
    private val sessionStore: SessionStore,
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

        // Fix: when using a custom Element Call URL, the Rust SDK may not correctly
        // append widget parameters to the generated URL. Detect this and manually
        // build the full URL with all required query parameters.
        if (!callUrl.contains("widgetId=")) {
            Timber.w("Element Call URL missing widget parameters, manually building URL")

            val intent = when {
                isDirect && hasActiveCall -> "join_existing_dm"
                hasActiveCall -> "join_existing"
                isDirect -> "start_call_dm"
                else -> "start_call"
            }

            val deviceId = matrixClient.deviceId.value
            val sessionData = sessionStore.getSession(sessionId.value)
            val homeserverUrl = sessionData?.homeserverUrl ?: ""

            val uriBuilder = Uri.parse(baseUrl).buildUpon()

            // Append /room path if not already present
            val basePath = Uri.parse(baseUrl).path ?: ""
            if (!basePath.contains("room")) {
                uriBuilder.appendPath("room")
            }

            uriBuilder
                .appendQueryParameter("widgetId", widgetSettings.id)
                .appendQueryParameter("parentUrl", "element://call")
                .appendQueryParameter("roomId", roomId.value)
                .appendQueryParameter("userId", sessionId.value)
                .appendQueryParameter("deviceId", deviceId)
                .appendQueryParameter("baseUrl", homeserverUrl)
                .appendQueryParameter("enableE2EE", if (isEncrypted) "true" else "false")
                .appendQueryParameter("perParticipantE2EE", if (isEncrypted) "true" else "false")
                .appendQueryParameter("confineToRoom", "true")
                .appendQueryParameter("appPrompt", "false")
                .appendQueryParameter("skipLobby", "true")
                .appendQueryParameter("intent", intent)

            languageTag?.let { uriBuilder.appendQueryParameter("lang", it) }
            theme?.let { uriBuilder.appendQueryParameter("theme", it) }

            callUrl = uriBuilder.build().toString()
            Timber.d("Built Element Call URL with widget parameters: $callUrl")
        }

        val driver = room.getWidgetDriver(widgetSettings).getOrThrow()

        CallWidgetProvider.GetWidgetResult(
            driver = driver,
            url = callUrl,
        )
    }
}
