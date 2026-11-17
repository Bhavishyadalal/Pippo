package com.mochyy.mochymusic.core.updater

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mochyy.mochymusic.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

const val GithubOwner = ""
const val GithubRepo = ""

const val GithubProfileUrl = ""
const val GithubRepoUrl = ""
const val InstagramUrl = ""
// Threads names follow the Instagram one
const val ThreadsUrl = ""
// dev spaces hides the discord row while this is blank
const val DiscordUrl = ""

data class GithubRelease(
    val title: String,
    val tag: String,
    val version: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSizeBytes: Long,
    val publishedAt: String,
    // marked as a prerelease on github, which is what makes it a nightly
    val isNightly: Boolean = false,
)

sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: GithubRelease) : UpdateState
    data class Failed(val message: String) : UpdateState
}

// a release is x.y.z, a nightly adds a fourth number counting up from the release it was built on (8.3.2.1, 8.3.2.2, then 8.3.3)
data class FullVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val buildNumber: Int = 0,
    val nightly: Int = 0,
) : Comparable<FullVersion> {
    override fun compareTo(other: FullVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (nightly != other.nightly) return nightly.compareTo(other.nightly)
        return buildNumber.compareTo(other.buildNumber)
    }

    // what people read: 8.3.2, 8.3.2.4 for a nightly
    val name: String get() = if (nightly > 0) "$major.$minor.$patch.$nightly" else "$major.$minor.$patch"
}

private val VersionRegex = Regex("""(\d+)\.(\d+)(?:\.(\d+))?(?:\.(\d+))?""")
private val BuildNumberRegex = Regex("""(?:build#|\(#|#|\()(\d+)\)?""", RegexOption.IGNORE_CASE)

fun parseFullVersion(text: String): FullVersion {
    val version = VersionRegex.find(text)?.groupValues
    val buildNumber = BuildNumberRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return FullVersion(
        major = version?.get(1)?.toIntOrNull() ?: 0,
        minor = version?.get(2)?.toIntOrNull() ?: 0,
        patch = version?.getOrNull(3)?.toIntOrNull() ?: 0,
        buildNumber = buildNumber,
        nightly = version?.getOrNull(4)?.toIntOrNull() ?: 0,
    )
}

/** Kept for compatibility with the existing update-state consumers. */
private class ParsedRelease(val version: FullVersion, val release: GithubRelease)

/** Every published release, asked for once and shared for [CacheTtlMillis] unless [force]d. */
private suspend fun getReleases(force: Boolean): Result<List<ParsedRelease>> {
    return Result.success(emptyList())
}

// release only sees releases; nightly sees the prereleases and the releases, and takes the newest of all
private fun ParsedRelease.inChannel(channel: UpdateChannel) = channel == UpdateChannel.Nightly || !release.isNightly

/** The newest release on [channel], whatever is installed. */
suspend fun getLatestRelease(channel: UpdateChannel, force: Boolean = false): Result<GithubRelease?> =
    getReleases(force).map { list -> list.filter { it.inChannel(channel) }.maxByOrNull { it.version }?.release }

/**
 * What "Downgrade to" offers, null when there's none: on release, the newest release before the
 * installed x.y.z (an older build of the same version doesn't count); on nightly, the nightly before
 * this one. A nightly never steps down to a release, that can break the library.
 */
suspend fun getPreviousRelease(channel: UpdateChannel, force: Boolean = false): Result<GithubRelease?> {
    val installed = parseFullVersion(BuildConfig.VERSION_NAME).copy(buildNumber = 0)
    return getReleases(force).map { list ->
        when {
            channel == UpdateChannel.Release && installedIsNightly -> null
            channel == UpdateChannel.Release -> list.filter { !it.release.isNightly && it.version.copy(buildNumber = 0) < installed }
            !installedIsNightly -> null
            else -> list.filter { it.release.isNightly && it.version.copy(buildNumber = 0) < installed }
        }?.maxByOrNull { it.version }?.release
    }
}

private suspend fun fetchReleases(): Result<List<ParsedRelease>> = withContext(Dispatchers.IO) {
    Result.success(emptyList())
}

fun isNewerThanInstalled(candidateVersionText: String): Boolean {
    val candidate = parseFullVersion(candidateVersionText)
    val installed = parseFullVersion(BuildConfig.VERSION_NAME)
    return candidate > installed
}

suspend fun downloadApk(
    context: Context,
    release: GithubRelease,
    onProgress: (Float) -> Unit,
): Result<File> = withContext(Dispatchers.IO) {
    Result.failure(IllegalStateException("Pippo updates are installed manually"))
}

fun apkFileFor(context: Context, release: GithubRelease): File =
    File(context.cacheDir, release.apkName ?: "pippo-update.apk")

fun isDownloaded(context: Context, release: GithubRelease): Boolean {
    val f = apkFileFor(context, release)
    if (!f.exists() || f.length() <= 0L) return false
    return release.apkSizeBytes <= 0L || f.length() == release.apkSizeBytes
}

fun canInstallPackages(context: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
        context.packageManager.canRequestPackageInstalls()

fun requestInstallPermission(context: Context) {
    // many tv boxes have no page for one app, so it falls back to the whole list, then security, then settings
    val pages = listOf(
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:" + context.packageName)),
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
        Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
        Intent(android.provider.Settings.ACTION_SETTINGS),
    )
    pages.firstOrNull { page ->
        runCatching { context.startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
}

fun installApk(context: Context, apk: File): Boolean {
    if (!canInstallPackages(context)) {
        requestInstallPermission(context)
        return false
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "unknown size"
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000f)
    else -> String.format("%.0f KB", bytes / 1000f)
}
