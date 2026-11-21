# Keep youtubedl-android reflection / Jackson models intact
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Keep Room entities and DAOs
-keep class com.vela.player.data.local.** { *; }

# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep OkHttp / Kotlin coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
