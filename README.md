# VelaPlayer

VelaPlayer is a standalone Android app for saving YouTube and Instagram media on the phone, then playing it offline. Downloads stay on the device. There is no account server.

The interface follows the Vela Emerald Slate design: dark `#09090B` / `#1E2230` / `#10B981`, and light `#FAFAFA` / `#FFFFFF` / `#10B981`.

## What it does

**Downloader.** Paste a link, or share one from YouTube or Instagram. Pick 360p, 720p, 1080p, audio only, or the original image when the post is a photo. YouTube videos, Shorts, Instagram Reels, Stories, single photos, and carousels use the same screen. Stories and restricted posts can open an in-app Instagram login. That session stays in private app storage.

**Library.** Saved videos, audio, and photos appear in an offline gallery. The app opens on the starred list. Switch to All to see everything, and filter by videos, audio, or images. Starred items and the last playback position are kept on the phone. The library can also import videos already stored on the device.

**Player.** Offline playback includes a seek bar, 10-second skips, double-tap seek (left half back, right half forward), previous and next, and a queue. The queue can play in order, shuffle, or repeat the current video. Speeds are 1x, 1.25x, 1.5x, 1.75x, 2x, 4x, 5x, and 10x. A screen lock ignores accidental touches. Picture-in-picture floats over the app and the Android home screen. A rotate button cycles the screen orientation.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17
- Android SDK 37

## Build

Open the project in Android Studio, or from this folder:

```bash
./gradlew assembleDebug
```

Debug APKs are written to `app/build/outputs/apk/debug/`. Use `app-arm64-v8a-debug.apk` on current phones. `app-universal-debug.apk` includes every supported CPU.

`local.properties` points at the Android SDK and is not part of the repository.

## Stack

Kotlin, Jetpack Compose, Room, DataStore, Media3 ExoPlayer, Coil, and an on-device `yt-dlp` plus `ffmpeg` build for downloads.

## Version

Current release: **0.1.0**. See [CHANGELOG.md](CHANGELOG.md).
