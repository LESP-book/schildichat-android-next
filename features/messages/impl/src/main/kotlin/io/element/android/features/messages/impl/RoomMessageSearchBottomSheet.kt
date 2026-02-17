package io.element.android.features.messages.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemAudioContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemFileContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemPollContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVoiceContent
import io.element.android.libraries.designsystem.components.SimpleModalBottomSheet
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RoomMessageSearchBottomSheet(
    state: MessagesState,
    onDismiss: () -> Unit,
    onOpenEvent: (EventId) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tab = SearchTab.entries.getOrElse(selectedTab) { SearchTab.Messages }
    val results = remember(state.timelineState.timelineItems, query, tab) {
        searchResults(
            timelineItems = state.timelineState.timelineItems,
            query = query,
            tab = tab,
        )
    }

    SimpleModalBottomSheet(
        title = stringResource(CommonStrings.action_search),
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(stringResource(R.string.sc_message_search_placeholder))
            },
        )
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = selectedTab) {
            SearchTab.entries.forEachIndexed { index, entry ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = stringResource(entry.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (tab == SearchTab.Messages && query.isBlank()) {
            Text(stringResource(R.string.sc_message_search_empty_query))
            return@SimpleModalBottomSheet
        }

        if (results.isEmpty()) {
            Text(stringResource(R.string.sc_message_search_no_results))
            return@SimpleModalBottomSheet
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(
                items = results,
                key = { it.eventId.value },
            ) { result ->
                ListItem(
                    headlineContent = {
                        Text(text = result.sender)
                    },
                    supportingContent = {
                        Text(
                            text = result.preview,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Text(text = result.sentTime)
                    },
                    modifier = Modifier.clickable {
                        onOpenEvent(result.eventId)
                    },
                )
            }
        }
    }
}

private enum class SearchTab(val titleRes: Int) {
    Messages(R.string.sc_message_search_tab_messages),
    Images(R.string.sc_message_search_tab_images),
    Files(R.string.sc_message_search_tab_files),
}

private data class SearchResult(
    val eventId: EventId,
    val sender: String,
    val preview: String,
    val sentTime: String,
    val sentTimeMillis: Long,
)

private data class SearchableContent(
    val tab: SearchTab,
    val searchableText: String,
    val previewText: String,
)

private fun searchResults(
    timelineItems: ImmutableList<TimelineItem>,
    query: String,
    tab: SearchTab,
): List<SearchResult> {
    val normalizedQuery = query.trim()

    return timelineItems
        .flatMap { timelineItem ->
            when (timelineItem) {
                is TimelineItem.Event -> listOf(timelineItem)
                is TimelineItem.GroupedEvents -> timelineItem.events
                is TimelineItem.Virtual -> emptyList()
            }
        }
        .mapNotNull { event ->
            event.toSearchableContent()?.let { content ->
                if (content.tab != tab) return@let null
                if (normalizedQuery.isNotBlank() && !content.searchableText.contains(normalizedQuery, ignoreCase = true)) return@let null
                if (tab == SearchTab.Messages && normalizedQuery.isBlank()) return@let null
                val eventId = event.eventId ?: return@let null
                SearchResult(
                    eventId = eventId,
                    sender = event.safeSenderName,
                    preview = content.previewText.ifBlank { content.searchableText },
                    sentTime = event.sentTime,
                    sentTimeMillis = event.sentTimeMillis,
                )
            }
        }
        .sortedByDescending { it.sentTimeMillis }
        .take(200)
}

private fun TimelineItem.Event.toSearchableContent(): SearchableContent? {
    return when (val eventContent = content) {
        is TimelineItemTextBasedContent -> {
            val text = eventContent.plainText.trim()
            SearchableContent(
                tab = SearchTab.Messages,
                searchableText = text,
                previewText = text,
            )
        }
        is TimelineItemPollContent -> {
            SearchableContent(
                tab = SearchTab.Messages,
                searchableText = eventContent.question,
                previewText = eventContent.question,
            )
        }
        is TimelineItemImageContent -> {
            val text = listOfNotNull(eventContent.caption, eventContent.filename).joinToString(" ").trim()
            SearchableContent(
                tab = SearchTab.Images,
                searchableText = text,
                previewText = eventContent.caption ?: eventContent.filename,
            )
        }
        is TimelineItemVideoContent -> {
            val text = listOfNotNull(eventContent.caption, eventContent.filename).joinToString(" ").trim()
            SearchableContent(
                tab = SearchTab.Images,
                searchableText = text,
                previewText = eventContent.caption ?: eventContent.filename,
            )
        }
        is TimelineItemFileContent -> {
            val text = listOfNotNull(eventContent.caption, eventContent.filename).joinToString(" ").trim()
            SearchableContent(
                tab = SearchTab.Files,
                searchableText = text,
                previewText = eventContent.caption ?: eventContent.filename,
            )
        }
        is TimelineItemAudioContent -> {
            val text = listOfNotNull(eventContent.caption, eventContent.filename).joinToString(" ").trim()
            SearchableContent(
                tab = SearchTab.Files,
                searchableText = text,
                previewText = eventContent.caption ?: eventContent.filename,
            )
        }
        is TimelineItemVoiceContent -> {
            val text = listOfNotNull(eventContent.caption, eventContent.filename).joinToString(" ").trim()
            SearchableContent(
                tab = SearchTab.Files,
                searchableText = text,
                previewText = eventContent.caption ?: eventContent.filename,
            )
        }
        else -> null
    }
}
