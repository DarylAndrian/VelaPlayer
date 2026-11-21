package com.vela.player.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans offline videos stored on the Android device (MediaStore) and registers them
 * in VelaPlayer's Room catalog without duplicating previously imported or downloaded items.
 */
class LocalMediaScanner(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    suspend fun scanDeviceVideos(): Int = withContext(Dispatchers.IO) {
        var newlyAdded = 0
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                ).toString()

                if (mediaDao.getByFilePath(uri) != null) continue

                val rawName = cursor.getString(nameCol) ?: "Device Video"
                val title = rawName.substringBeforeLast('.')
                val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                val height = cursor.getInt(heightCol)
                val dateAddedSec = cursor.getLong(dateCol)

                val entity = MediaItemEntity(
                    title = title,
                    filePath = uri,
                    thumbnailPath = uri,
                    mediaType = MediaType.VIDEO.name,
                    sourcePlatform = MediaSource.LOCAL.name,
                    resolutionLabel = if (height > 0) "${height}p" else "Video",
                    durationMs = duration,
                    fileSizeBytes = size,
                    isStarred = false,
                    createdAtEpochMs = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                )
                val insertedId = mediaDao.insertIgnore(entity)
                if (insertedId != -1L) newlyAdded++
            }
        }
        newlyAdded
    }
}
