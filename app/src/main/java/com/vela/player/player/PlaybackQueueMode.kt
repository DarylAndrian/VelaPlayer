package com.vela.player.player

enum class PlaybackQueueMode(val label: String, val detail: String) {
    IN_ORDER(
        label = "In order",
        detail = "Play the next video when this one ends"
    ),
    SHUFFLE(
        label = "Shuffle",
        detail = "Play another video at random when this one ends"
    ),
    REPEAT_ONE(
        label = "Repeat",
        detail = "Replay this video from the start, over and over"
    );

    companion object {
        fun fromStored(value: String?): PlaybackQueueMode =
            entries.firstOrNull { it.name == value } ?: IN_ORDER
    }
}
