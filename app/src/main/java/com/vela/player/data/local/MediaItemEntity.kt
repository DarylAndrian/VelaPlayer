package com.vela.player.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    IMAGE("Image");

    companion object {
        fun fromString(value: String): MediaType =
            entries.firstOrNull { it.name == value } ?: VIDEO
    }
}

enum class MediaSource(val label: String) {
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram"),
    LOCAL("Device");

    companion object {
        fun fromString(value: String): MediaSource =
            entries.firstOrNull { it.name == value } ?: LOCAL
    }
}

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["isStarred"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val mediaType: String = MediaType.VIDEO.name,
    val sourcePlatform: String = MediaSource.LOCAL.name,
    val sourceUrl: String? = null,
    val resolutionLabel: String? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val isStarred: Boolean = false,
    val lastPositionMs: Long = 0L,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val parsedMediaType: MediaType get() = MediaType.fromString(mediaType)
    val parsedSource: MediaSource get() = MediaSource.fromString(sourcePlatform)
}
