package com.vela.player.data.downloader

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import com.vela.player.data.local.MediaDao
import com.vela.player.data.local.MediaItemEntity
import com.vela.player.data.local.MediaSource
import com.vela.player.data.local.MediaType
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Coordinates queued and active on-device downloads using yt-dlp + ffmpeg (video/audio)
 * and OkHttp (Instagram images/carousels), persisting finished files into Room.
 */
class DownloadRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val cookieStore: InstagramCookieStore,
    private val instagramExtractor: InstagramMediaExtractor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeCoroutines = mutableMapOf<String, Job>()

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    fun enqueueDownload(analyzed: AnalyzedMedia, option: DownloadOption): DownloadJob {
        val jobId = UUID.randomUUID().toString()
        val job = DownloadJob(
            id = jobId,
            title = analyzed.title,
            sourceUrl = analyzed.url,
            platform = analyzed.platform,
            option = option,
            thumbnailUrl = analyzed.thumbnailUrl,
            imageUrls = analyzed.imageUrls,
            status = DownloadStatus.QUEUED,
            statusDetail = "Starting ${option.label}..."
        )
        _jobs.update { listOf(job) + it }
        DownloadForegroundService.startForJob(context, jobId)
        return job
    }

    fun executeJob(jobId: String, onProgressNotify: (DownloadJob) -> Unit, onAllIdle: () -> Unit) {
        val existing = _jobs.value.firstOrNull { it.id == jobId } ?: run {
            onAllIdle()
            return
        }
        if (activeCoroutines.containsKey(jobId)) return

        val coroutineJob = scope.launch {
            try {
                if (existing.option.isImage) {
                    runImageDownload(existing, onProgressNotify)
                } else {
                    runYtDlpDownload(existing, onProgressNotify)
                }
            } catch (ce: CancellationException) {
                updateJob(jobId) {
                    it.copy(status = DownloadStatus.FAILED, statusDetail = "Cancelled")
                }
            } catch (t: Throwable) {
                val msg = t.message?.take(140) ?: "Download failed"
                updateJob(jobId) {
                    it.copy(status = DownloadStatus.FAILED, statusDetail = msg)
                }
            } finally {
                activeCoroutines.remove(jobId)
                if (activeCoroutines.isEmpty()) {
                    onAllIdle()
                }
            }
        }
        activeCoroutines[jobId] = coroutineJob
    }

    fun cancelJob(jobId: String) {
        YoutubeDL.getInstance().destroyProcessById(jobId)
        activeCoroutines.remove(jobId)?.cancel()
        updateJob(jobId) {
            it.copy(status = DownloadStatus.FAILED, statusDetail = "Cancelled by user")
        }
    }

    fun dismissJob(jobId: String) {
        _jobs.update { list -> list.filterNot { it.id == jobId } }
    }

    private suspend fun runImageDownload(
        job: DownloadJob,
        onProgressNotify: (DownloadJob) -> Unit
    ) {
        updateJob(job.id) {
            it.copy(status = DownloadStatus.DOWNLOADING, progress = 5f, statusDetail = "Fetching images...")
        }
        val imageUrls = job.imageUrls.ifEmpty {
            instagramExtractor.extractImages(job.sourceUrl).imageUrls
        }
        if (imageUrls.isEmpty()) {
            throw IllegalStateException("No downloadable Instagram images found. Try logging in to Instagram.")
        }

        val outDir = getMediaOutputDir("Images")
        val safeBase = sanitizeFilename(job.title)
        var savedCount = 0
        var lastPath: String? = null

        imageUrls.forEachIndexed { index, imgUrl ->
            val file = File(outDir, "${safeBase}_${System.currentTimeMillis()}_${index + 1}.jpg")
            val ok = instagramExtractor.downloadImageToFile(imgUrl, file)
            if (ok && file.exists()) {
                savedCount++
                lastPath = file.absolutePath
                scanMediaFile(file)
                mediaDao.upsert(
                    MediaItemEntity(
                        title = if (imageUrls.size > 1) "${job.title} (${index + 1}/${imageUrls.size})" else job.title,
                        filePath = file.absolutePath,
                        thumbnailPath = file.absolutePath,
                        mediaType = MediaType.IMAGE.name,
                        sourcePlatform = MediaSource.INSTAGRAM.name,
                        sourceUrl = job.sourceUrl,
                        resolutionLabel = "Image",
                        fileSizeBytes = file.length(),
                        createdAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
            val pct = ((index + 1).toFloat() / imageUrls.size.toFloat()) * 100f
            updateJob(job.id) {
                it.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = pct,
                    statusDetail = "Saved $savedCount of ${imageUrls.size} images"
                )
            }.also(onProgressNotify)
        }

        if (savedCount == 0) {
            throw IllegalStateException("Could not save Instagram image files.")
        }

        updateJob(job.id) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100f,
                statusDetail = "Saved $savedCount image${if (savedCount > 1) "s" else ""}",
                outputPath = lastPath
            )
        }.also(onProgressNotify)
    }

    private suspend fun runYtDlpDownload(
        job: DownloadJob,
        onProgressNotify: (DownloadJob) -> Unit
    ) {
        val folderName = if (job.option.isAudioOnly) "Audio" else "Videos"
        val outDir = getMediaOutputDir(folderName)
        val stamp = System.currentTimeMillis()
        val safeTitle = sanitizeFilename(job.title)
        val outputTemplate = File(outDir, "${safeTitle}_$stamp.%(ext)s").absolutePath

        val request = YoutubeDLRequest(job.sourceUrl).apply {
            addOption("-o", outputTemplate)
            addOption("--no-playlist")
            addOption("--no-mtime")
            if (job.platform == DownloadPlatform.DAILYMOTION) {
                addOption("--concurrent-fragments", "4")
            }
            if (job.platform == DownloadPlatform.INSTAGRAM) {
                cookieStore.getCookieFileIfPresent()?.let { cookieFile ->
                    addOption("--cookies", cookieFile.absolutePath)
                }
            }
            if (job.option.isAudioOnly) {
                addOption("-x")
                addOption("--audio-format", "m4a")
                addOption("--embed-thumbnail")
                addOption("--add-metadata")
            } else {
                val maxH = job.option.maxHeight ?: 720
                addOption(
                    "-f",
                    "bestvideo[height<=?$maxH][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=?$maxH]+bestaudio/best[height<=?$maxH]/best"
                )
                addOption("--merge-output-format", "mp4")
            }
        }

        updateJob(job.id) {
            it.copy(status = DownloadStatus.DOWNLOADING, progress = 1f, statusDetail = "Connecting...")
        }.also(onProgressNotify)

        val beforeFiles = outDir.listFiles()?.map { it.absolutePath }?.toSet().orEmpty()

        YoutubeDL.getInstance().execute(request, job.id) { progress, etaSeconds, _ ->
            val clamped = progress.coerceIn(0f, 100f)
            val status = if (clamped >= 99.5f) DownloadStatus.MERGING else DownloadStatus.DOWNLOADING
            val detail = if (status == DownloadStatus.MERGING) {
                "Muxing media..."
            } else if (etaSeconds > 0) {
                "${clamped.toInt()}% • ${etaSeconds}s left"
            } else {
                "${clamped.toInt()}%"
            }
            updateJob(job.id) {
                it.copy(status = status, progress = clamped, statusDetail = detail)
            }.also(onProgressNotify)
        }

        val producedFile = outDir.listFiles()
            ?.filter { it.isFile && it.absolutePath !in beforeFiles && !it.name.endsWith(".part") }
            ?.maxByOrNull { it.lastModified() }
            ?: outDir.listFiles()
                ?.filter { it.isFile && it.name.contains(stamp.toString()) && !it.name.endsWith(".part") }
                ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("Download completed but output file was not found.")

        scanMediaFile(producedFile)
        val durationMs = extractDurationMs(producedFile)

        val mediaEntity = MediaItemEntity(
            title = job.title,
            filePath = producedFile.absolutePath,
            thumbnailPath = if (job.option.isAudioOnly) job.thumbnailUrl else producedFile.absolutePath,
            mediaType = if (job.option.isAudioOnly) MediaType.AUDIO.name else MediaType.VIDEO.name,
            sourcePlatform = when (job.platform) {
                DownloadPlatform.YOUTUBE -> MediaSource.YOUTUBE.name
                DownloadPlatform.INSTAGRAM -> MediaSource.INSTAGRAM.name
                DownloadPlatform.DAILYMOTION -> MediaSource.DAILYMOTION.name
                DownloadPlatform.UNSUPPORTED -> MediaSource.LOCAL.name
            },
            sourceUrl = job.sourceUrl,
            resolutionLabel = job.option.label,
            durationMs = durationMs,
            fileSizeBytes = producedFile.length(),
            createdAtEpochMs = System.currentTimeMillis()
        )
        mediaDao.upsert(mediaEntity)

        updateJob(job.id) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100f,
                statusDetail = "Saved to Offline Gallery",
                outputPath = producedFile.absolutePath
            )
        }.also(onProgressNotify)
    }

    private fun updateJob(jobId: String, transform: (DownloadJob) -> DownloadJob): DownloadJob {
        var updated: DownloadJob? = null
        _jobs.update { list ->
            list.map { item ->
                if (item.id == jobId) {
                    transform(item).also { updated = it }
                } else {
                    item
                }
            }
        }
        return updated ?: _jobs.value.first { it.id == jobId }
    }

    private fun getMediaOutputDir(subFolder: String): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val preferred = File(publicDownloads, "VelaPlayer/$subFolder")
        if (preferred.exists() || preferred.mkdirs()) {
            if (preferred.canWrite()) return preferred
        }
        // Scoped storage safe fallback inside app external files
        val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "VelaPlayer/$subFolder")
        fallback.mkdirs()
        return fallback
    }

    private fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scanMediaFile(file: File) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
    }

    private fun sanitizeFilename(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(56)
        return cleaned.ifBlank { "VelaMedia" }
    }

    suspend fun updateYtDlpEngine(): YoutubeDL.UpdateStatus? = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
    }
}
