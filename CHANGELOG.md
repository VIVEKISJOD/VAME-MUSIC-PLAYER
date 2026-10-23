# VAME Music - full changelog (up to zip step23, v0.11.0, 24 Oct 2026)

## step23 (v0.11.0)
- Changed: the big "Last Played" banner is gone from Home (phone and tablet). A Liked songs shelf (big cards, See all) takes its place. With no likes yet it shows songs for you.
- New: Settings > App icon. 8 logo variants (Classic, Crimson, Sunset, Emerald, Ocean, Violet, Midnight, Mono). Tap one and the home screen icon changes (some launchers need a few seconds or close the app once).
- New: animations when things close. Dialogs (new playlist, add to playlist, sleep timer, audio output, import) now fade and scale in AND out. The page under a playlist / album / artist / liked / downloads page shrinks and dims while it is open and eases back on Back. Recap slides up and down.
- Changed: the bottom bar fold is smoother. The spring is only read while drawing (no re-composing each frame), the mini player no longer replays its entrance after unfolding, and the hidden half cannot be tapped by mistake.
- New: Explore (replaces Search). Big title, search box and colourful mood tiles (Chill, Commute, Energize, Feel good, Focus, Gaming, Party, Romance, Sad, Sleep). Tap a tile to see those songs. Back or the X in the search box returns to the tiles. Your genres tiles stay below.
- New: Library redone. Swipeable stats cards (minutes listened, top artist, top song), Downloads / Liked songs / Your Recap rows, big tiles for Playlists (New, Import, Liked Songs + yours, with Show all), Recently played and Artists, and a history + profile pill on top.
- New: Recap. A Spotify-style story: intro, minutes listened (counts up), top artist, top 5 songs, top 5 artists, summary with Share. Tap right/left to move, it also moves by itself.
- New: listening stats. VAME counts the seconds you really listen per song and artist (saved on the phone only). Recap and the Library cards use it. Counting starts with this build.

## step22 (v0.10.1)
- New: Settings > Appearance. Pick Colour glass, AMOLED (pure black) or AMOLED glass (pure black + glass), choose a palette (Crimson, Sunset, Amber, Emerald, Ocean, Violet, Rose, Mono) or make your own colour with the sliders. The whole app changes and it is remembered.
- Changed: the bottom bar fold is rebuilt to be smooth (one spring for fade, slide, scale and height) and needs a real swipe so it no longer flickers.
- New: more animations: tab switching slides and fades, pages open with an ease-out slide, songs and playlist rows rise in, mini player springs in, play/pause icon fades.
- (tablet layout, glass, logo, update fix, bar fold below)

## step22 (v0.10.0)
- New: tablet layout. Navigation rail on the left (Home, Search, Your type, Library), Now Playing and lyrics open in a glass panel on the right while the pages stay usable, and a floating glass mini player at the bottom.
- New: glass look everywhere: songs, cards, buttons, sheets, dialogs and the rail now have glass fill, a light edge and a sheen. The page behind a sheet or dialog is blurred (Android 12+).
- New: bigger Home banner on tablets; sheets and dialogs stay a nice width on tablets.
- New: the new VAME logo is the app icon and shows on the tablet rail.
- (also in this build) bottom bar fold animation and the update download fix, listed below.

## step22 (v0.9.7)
- New: bottom bar animation. Scroll down and the mini player + tab bar fold into one slim row (Home, mini player, Search). Scroll up and it opens again. Tapping Home or Search opens it back.
- Fixed: update download no longer freezes half way. If the connection drops or stalls, VAME waits and continues from where it stopped (up to 8 tries), on a steadier connection mode.

## step21 (v0.9.6)
- Test update: checks that VAME can update itself with the new download bar. Nothing else changed.

## step20 (v0.9.5)
- Fixed: the update download no longer gets stuck on "Waiting to connect". VAME now downloads the update itself, with a progress bar in the update box, then opens the installer.
- New: if the download fails, the box shows the reason and a Try again button.

## step19 (v0.9.4)
- Changed: Now Playing now matches your screenshot: full-width artwork at the top that fades into the background, big title with artist under it, star and 3-dot menu on the right.
- New: the current lyric line is big and bold under the song name (tap it for full lyrics).
- New: thick progress bar with time passed, audio quality (kbps / kHz) in the middle and time left.
- New: big rewind / play / forward buttons and a thick volume bar.
- New: bottom row with lyrics, a pill with sound output + repeat, and queue; the output name is shown under it.
- Changed: like is a star now. Tap the artwork to turn the moving cover (Animate cover) on or off. Drag down to close.

## step18 (v0.9.3)
- Fixed: covers are sharp now. Big covers (player, album, artist, playlist, home) load in high resolution instead of the small blurry picture.
- New: Now Playing looks like iOS: drag handle on top, big artwork with shadow, title and artist on the left, heart and ... on the right.
- New: artwork shrinks when the song is paused and grows when it plays.
- New: one line of lyrics under the song name, changes with the song (tap it to open full lyrics).
- New: thin iOS-style progress bar that grows when you touch it, with time left (-1:23).
- New: volume slider with speaker icons (real phone volume).
- New: drag the player down to close it. Tap the artwork for Animate cover.
- Changed: bottom icons are now lyrics, sound output, repeat, queue.

## step17 (v0.9.2)
- New: automatic updates. VAME checks GitHub for a newer version when it opens and in Settings > Check for updates. Tap Update now to download and install it over the old version.
- New: every upload to the repo now publishes a signed release (Releases page) that users download and the app updates from.

## step16 (v0.9.1)
- Changed: playlist covers are big square tiles in Library (2 per row on phone, 4 on tablet) and the playlist page shows a big cover with the name.
- Changed: New playlist and Import playlist are now two buttons above the playlists.

## step15 (v0.9.0)
- New: VAME logo is now the app icon.
- New: Settings > About has "Made by vxvekggs", a Follow on Instagram button and a Source code button.
- New: open-source repo files (README, GPL-3.0 license, third-party licenses, disclaimer, privacy, contributing, security, issue templates).
- New: automatic repo cleanup + build: uploading a zip unpacks it, deletes old zips and builds the APK.
- Changed: removed "Powered by AmeOS" (the app is made by vxvekggs, only inspired by AmeOS).

## step14 (v0.8.0)
- Fixed: Back on Search / Your Type / Library now goes to Home instead of leaving the app.
- Fixed: Home shows only music: no "Top 100 hits" style compilations, no hour-long uploads, no random-creator albums.
- Fixed: Up next, radio and endless queue use music-only songs (same artist + similar music), not random YouTube videos.
- New: faster playlist import (8 songs searched at once, progress x/y, YouTube playlists up to 500 songs).
- New: import from Spotify short links (spotify.link), Spotify albums, and shared text that contains a link.
- New: import by pasting a list of songs (one per line) from any app.
- New: import box fills itself from a copied link and has an Open playlist button when done.

## step13 (v0.7.0)
- Fixed: Xiaomi island / notification no longer disappears after the player stays paused for a long time (notification stays foreground; Settings switch + battery shortcut).
- New: Home, Search, Your Type, Library redone like the Figma screenshots.
- New: glass look and smoother press animations.
- New: Album pages (Home, Search albums row).
- New: Offline downloads (Download button, Downloads page, offline playback, download quality).
- New: Import playlist from YouTube Music / YouTube / Spotify link.
- New: Animate cover button on Now Playing.

## step12 (v0.6.0)
- New: shuffle button in Up next (shuffles only the songs after the current one).
- Fixed: liking a song no longer adds its artist to Liked Artists. Artists are liked only with the Like artist button on their page.
- New: artist page like Spotify (photo, Like artist, Play, Shuffle, all songs).
- New: Go to artist opens the artist page (... menu, long-press sheet, artist name on Now Playing).
- New: full Liked songs page (Library > See all) with Play and Shuffle.
- New: long-press any song in Up next for Play next / Add to playlist / Add to queue.

## step11 (v0.5.1)
- Fixed build error from step10 (functions renamed, no visible change).
- New: playback speed (0.75x-2x), skip silence switch, resume last song on app open.

## step10 (v0.5.0) - Build A
- Fixed: bottom-left label shows Bluetooth device name, not "This phone".
- New: choose sound output (phone / Bluetooth / headphones); swipe from recents stops the app and music (switch); Hinglish lyrics; Settings: audio quality, clear cache, clear history; Your Type rebuilt from your listening and genres with refresh button.

## step9 (v0.4.3)
- New: Official songs only (hides remixes, slowed + reverb, lofi, covers, lyrics-channel uploads) with a Settings switch.

## step8 (v0.4.2)
- New: long-press any song = Play next / Add to playlist / Add to queue sheet. Last played saved on the phone.

## step7 (v0.4.1)
- Fixed build error (duplicate import). Removed Watch video. New: first-launch taste page (16 genres), taste feeds Home and Your Type, Settings > Change music taste.

## step6 (v0.4.0)
- Fixed: tapping the notification / lock screen / island opens the app. New: real playlists with name + cover photo, playlist page, Add to playlist works.

## step5 (v0.3.3)
- Quick sheet drags closed and Back closes it first; sleep dialog redesigned; cover-colour background on Now Playing and Lyrics; drag to reorder Up next; play-next button on every row.

## step4 (v0.3.2)
- Lyrics top pill returns to cover; new ... quick settings bottom sheet.

## step3 and earlier (v0.3 and before)
- Notification/lock screen/island fixed; Home chips and settings icon work; Now Playing + synced Lyrics; loop; sleep timer; endless queue; Up next; Suggested for you; Settings page; likes saved; animations; faster playback; custom sleep timer and "play N times".
