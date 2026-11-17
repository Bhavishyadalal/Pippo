// repo numbers for dev spaces straight from github, if github refuses (rate limit, offline) the last good numbers stay

package com.mochyy.mochymusic.ui.screens.settings.devspace

import com.mochyy.mochymusic.core.updater.GithubOwner
import com.mochyy.mochymusic.core.updater.GithubRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeFormatter

data class DevIssue(
    val number: Int,
    val title: String,
    val open: Boolean,
    val labels: List<String>,
    val url: String,
    val createdAt: Instant,
) {
    /** a "bug" label, or the word in the title, reporters don't always label */
    val isBug: Boolean = labels.any { it.contains("bug", true) } || title.contains("bug", true)

    val isFeature: Boolean = !isBug && (
        labels.any { it.contains("enhancement", true) || it.contains("feature", true) } ||
            title.contains("feature", true) || title.contains("request", true)
        )
}

data class DevSpaceStats(
    val openTotal: Int = 0,
    val closedTotal: Int = 0,
    val newIssues: Int? = null,
    val newResolved: Int? = null,
    val commits: Int? = null,
    val releases: Int? = null,
    val issues: List<DevIssue> = emptyList(),
    val fetchedAt: Long = 0L,
) {
    val reportedTotal: Int get() = openTotal + closedTotal
}

// Repository statistics are intentionally offline in Pippo.
object DevSpaceRepository {
    val current: StateFlow<DevSpaceStats?> = MutableStateFlow<DevSpaceStats?>(null).asStateFlow()
    val loading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    val error: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    suspend fun refresh(sinceMillis: Long, force: Boolean, maxAgeMs: Long = 60_000L) = Unit
}
