<p align="center">
  <img src="docs/logo.png" width="140" alt="VAME Music logo">
</p>

<h1 align="center">VAME Music</h1>

<p align="center">
  A free, open-source music player for Android. No ads, no accounts, no tracking.<br>
  Made by <a href="https://www.instagram.com/vxvekggs/">@vxvekggs</a>
</p>

---

## Features

- Search and play songs, with a full Now Playing page and an Up next queue
- Lyrics: synced, with word glow, translation, Hinglish and a size slider. Sources: LRCLIB, NetEase, Kugou and Lyrics.ovh. If a song has no lyrics, VAME keeps looking in the background while it plays. Controls hide by themselves after 5 seconds
- Home with YouTube Music shelves, your genres, Recents and a speed dial for pinned playlists, albums, artists and songs
- Artist pages with top songs, albums, singles and EPs, playlists, similar artists and About; album and playlist pages with a full menu (sort, find, mix, save)
- Your Type: suggestions built from what you listen to and the genres you pick
- Playlists with your own name and cover photo, liked songs, liked artists
- Import a playlist from YouTube Music, YouTube or Spotify (link), or paste a list of songs
- Optional Google sign-in to see your YouTube Music library
- Offline downloads with a choice of quality
- Sleep timer, play N times, loop, shuffle, playback speed, skip silence
- Choose the sound output (phone, Bluetooth, headphones) and play on another VAME device on the same Wi-Fi
- Standby player with a clock, home screen widgets, glass look and many Settings
- Phone, tablet and Android TV in the same app
- "Official songs only" switch that hides remixes, slowed + reverb and lyric-channel uploads
- Lock screen / notification controls

See [CHANGELOG.md](CHANGELOG.md) for everything, version by version.

## Install

1. Open the **Releases** page (right side of this repo) and download the newest `VAME-x.y.z.apk`.
2. Open it and install (allow "install from unknown sources" when Android asks).

Needs Android 8.0 (API 26) or newer.

**Updates:** VAME checks for a new release when you open it (and in Settings > Check for updates).
When one is out, tap **Update now** and it downloads and installs over your old version. Your playlists and likes stay.

## Build it yourself

You only need GitHub: push to `main` and the workflow in `.github/workflows/build.yml` builds the APK for you.

On a computer: install JDK 17 and Gradle 8.10, then run `gradle assembleDebug`.
The APK is created in `app/build/outputs/apk/debug/`.

## Built with

Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer), Coil, OkHttp and
[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor).
Full list with licenses: [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

## Please read

- VAME is **not affiliated with YouTube, Google, Spotify or Apple**. See [DISCLAIMER.md](DISCLAIMER.md).
- Your data stays on your phone. See [PRIVACY.md](PRIVACY.md).
- Want to help or report a problem? See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).

## License

VAME Music is free software under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)).

## Follow

Instagram: [@vxvekggs](https://www.instagram.com/vxvekggs/)
