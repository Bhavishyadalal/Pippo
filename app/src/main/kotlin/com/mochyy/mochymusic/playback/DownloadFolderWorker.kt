package com.mochyy.mochymusic.playback

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadFolderEntryPoint {
    fun downloadExporter(): DownloadExporter
}

/** Copies offline audio to Files after Media3 finishes writing it. Work survives app restarts. */
class DownloadFolderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(MediaId) ?: return@withContext Result.failure()
        val folder = inputData.getString(Folder) ?: return@withContext Result.failure()
        val tree = folder.toUri()
        if (applicationContext.contentResolver.persistedUriPermissions.none {
                it.uri == tree && it.isReadPermission && it.isWritePermission
            }
        ) return@withContext Result.failure()

        try {
            val exporter = EntryPointAccessors
                .fromApplication(applicationContext, DownloadFolderEntryPoint::class.java)
                .downloadExporter()
            exporter.copyCompletedDownload(tree, id)
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w("DownloadFolder", "Couldn't copy the completed download", exception)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MediaId = "mediaId"
        private const val Folder = "folder"

        fun tag(folder: String): String = "pippo-download-folder-${UUID.nameUUIDFromBytes(folder.toByteArray())}"

        fun enqueue(context: Context, mediaId: String, folder: String) {
            if (folder.isBlank()) return
            val tag = tag(folder)
            val request = OneTimeWorkRequestBuilder<DownloadFolderWorker>()
                .setInputData(workDataOf(MediaId to mediaId, Folder to folder))
                .addTag(tag)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("$tag-$mediaId", ExistingWorkPolicy.KEEP, request)
        }
    }
}
