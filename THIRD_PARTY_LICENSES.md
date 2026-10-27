# Third-party software and services

VAME Music is built on the work of others. Thank you.

| Project | Used for | License |
|---|---|---|
| [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (TeamNewPipe) | Finding and streaming songs | GPL-3.0 |
| [AndroidX Media3](https://github.com/androidx/media) | Audio playback, notification, media session | Apache-2.0 |
| [Jetpack Compose / AndroidX](https://developer.android.com/jetpack) | User interface | Apache-2.0 |
| [Coil](https://github.com/coil-kt/coil) | Loading cover images | Apache-2.0 |
| [OkHttp](https://github.com/square/okhttp) | Network requests | Apache-2.0 |
| [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | Playing on another VAME device (same Wi-Fi) | BSD-3-Clause |
| [Kotlin and kotlinx.coroutines](https://github.com/JetBrains/kotlin) | Programming language and async code | Apache-2.0 |

NewPipeExtractor brings in its own libraries. Their licenses are listed in the NewPipeExtractor repository.

## Services

- **[LRCLIB](https://lrclib.net)** (free, no key), **NetEase Cloud Music**, **Kugou** and **Lyrics.ovh** provide lyrics. **Google Translate** (web endpoint) translates them. Lyrics belong to their respective rights holders. VAME fetches them while you listen and does not store or redistribute them in this repository.
- Song information, audio streams and cover images are loaded from YouTube / YouTube Music while the app runs. No audio or artwork is stored in this repository.

## Design

The VAME logo and name belong to the project owner (@vxvekggs).
