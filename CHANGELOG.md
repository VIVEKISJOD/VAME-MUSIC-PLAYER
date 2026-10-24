# VAME Music - full changelog (up to zip step34, v0.18.2, 24 Oct 2026)

## step34 (v0.18.2) - includes step28 fix and steps 29 to 33
- Fixed: the update window looked stuck on "Downloading..." because the percentage was hidden under the long notes. Progress now sits at the top of the window, always visible.
- New: shows how much is downloaded, e.g. "12.4 / 31.0 MB - 450 KB/s".
- New: Cancel button while downloading.
- New: "Download in browser instead" button in the update window (backup way).
- New: "Try again" continues the half-finished download instead of starting from 0.
- New: server errors (5xx) and dropped connections retry up to 12 times and show "Connection problem, trying again (n/12)".
- New: a note to keep VAME open on screen while it downloads.

## step33 (v0.18.1) - includes step28 fix and steps 29 to 32
- Changed: a tablet held upright (portrait) now works like a normal phone: bottom bar, mini player pill and full screen player. The side rail and side player only show when the tablet is sideways.
- New: tablet sideways with the side player closed: a small floating box with the song cover (replaces the pill). Tap it to open the side player.
- New: the box can be dragged anywhere. When you let go it snaps to the left or right edge.
- New: the box remembers its side and height after closing the app.
- New: play / pause button on the box.
- New: the box hides while the side player or lyrics are open and comes back when they close.
- Fixed: the pill no longer shows on any page on a sideways tablet (the box takes its place).

## step32 (v0.18.0) - includes step28 fix, step29, step30, step31
- New: Home now has a full library of shelves built from the genres you picked when you signed up (up to 6 genres). Each genre gets its own row of big song cards, up to 20 songs.
- New: every genre shelf mixes English and Hindi songs.
- New: Bollywood, Punjabi, K-pop and Devotional shelves use their own searches (Hindi, Punjabi, Korean, bhajans).
- New: Settings > Home songs language: English + Hindi, English only or Hindi only.
- New: shelves load one by one, so Home fills up while the rest is still loading.
- Changed: if you picked no genres, Home shows Pop, Hip-Hop and Bollywood shelves. Changing your music taste in Settings reloads the shelves.

## step31 (v0.17.1) - includes step28 fix, step29 and step30
- Changed: Home is bigger. The Liked songs shelf cards, Last played albums and Recommended Albums covers and text are larger (phone and tablet).
- New: Home shows 6 recommended items instead of 4.
- New: "Quick picks" shelf of big cards on Home (more songs to tap).
- New: "Your artists" row of round artist pictures on Home (artists you liked), tap to open the artist page.
- New: bigger spacing between Home rows so the page fills the screen.

## step30 (v0.17.0) - includes step28 fix and step29
- Fixed: the step28 build error (Platform declaration clash on setArtistLatestPic / setArtistAlbumCardOn in MainViewModel). Functions renamed, no visible change.
- New: when you are signed in with Google, your profile picture shows top right on Home, Explore and Library instead of the plain account icon (tap it for Settings).
- New: playlists you make in VAME are also saved to your YouTube Music account as private playlists.
- New: songs you add to a VAME playlist are added on YouTube Music too. Renaming or deleting a VAME playlist does the same there. Only playlists VAME made are touched.
- New: imported playlists (YouTube / Spotify link, pasted list) are saved to YouTube Music too.
- New: Settings > Account > "Save new playlists to YouTube Music" ON/OFF switch (default ON).
- New: Settings > Account > "Upload my VAME playlists to YouTube Music" sends the playlists you already have.
- Changed: a playlist VAME created on YouTube Music is not shown twice (Library and Home > Playlists show only the VAME copy).
- Includes step29: no mini player pill on tablet pages while the side player is open, and Home > Playlists shows your real playlists as a grid.

## step29 (v0.16.1)
- Fixed: on a tablet the mini player pill no longer shows on the artist, playlist, album, liked or downloads pages while the player panel is open on the right.
- Fixed: Home > Playlists now shows your real playlists (VAME playlists and, if signed in, your YouTube Music playlists), not the three fake Daily / Bangers / Evening boxes.
- New: Home > Playlists is a grid of cover tiles (2 per row on phone, 4 on tablet) like the Library.
- New: Home > Playlists has Liked Songs, New playlist and Import playlist tiles at the top.
- New: tapping the Playlists chip loads your YouTube Music playlists if you are signed in and the list is empty.
- New: the Playlists title shows how many playlists you have, and an empty list shows a short hint.

## step28 (v0.16.0) - includes everything from step27
- New: the Now Playing screen opens out of the mini player pill and, when you go back, it collapses smoothly back into the pill (soft spring, corners round off while it is small).
- New: the same grow-and-shrink animation for Lyrics (from the lyrics button corner), Settings (from the top right), Recap and Sign in with Google.
- Changed: the Play Protect help text now matches the real "App scan recommended" dialog (tap Scan app, wait a few seconds, then install) and says why it appears.
- Note about Play Protect: it cannot be switched off from the app. It shows because Google has not seen this exact APK on many phones yet. Other open source apps are installed by thousands of people, so Google already knows their files. Every new VAME version is a new file, so it can show again after an update until enough phones have installed it. Real fixes: tap Scan app once, turn off Scan apps with Play Protect, or put VAME on the Play Store.

## step27 (v0.15.0)
- New: the artist page picture is now the artist's latest release. VAME finds the newest official upload of the artist and uses its big picture at the top (same iOS style look: fades into the page). If it cannot find one it uses the song cover.
- New: a "Latest: song name" pill under the artist name. Tap it to play the latest release.
- New: Settings > Artist page > Picture (Latest release or Song cover).
- New: Settings > Artist page > Popular album card on or off.
- New: Settings > Artist page > Top songs shown (10, 25 or All).
- Changed: Share on the artist page also says the latest release name.

## step26 (v0.14.0)
- Fixed: on a tablet the Now Playing panel on the right now fills the whole right side (touches the top, right and bottom edges, no empty corner).
- Changed: artist page redesigned. Big picture at the top that fades into the page, round back and share buttons floating on it, big artist name, a row with shuffle, a big play button and a star (favourite artist), a "Popular album" glass card with a + (save album) button, then "Top songs".
- New: Play Protect help text in the update dialog and in Settings > Updates (how to continue the install and how to turn the warning off). The warning itself comes from Google because VAME is not from the Play Store; the app cannot hide it.
- New: Settings > Open app on (Home, Explore, Your type, Library).
- New: Settings > Tablet player width (Normal or Wide).
- New: Settings > Lyrics alignment (Centered or Left).
- New: the YouTube Music row in Library loads by itself when you are signed in but the list is empty.
- Settings chips can scroll sideways when they do not fit.

## step25 (v0.13.0)
- Fixed: tablet logo (top left of the side rail) now changes together with the app icon you pick in Settings > App icon.
- Fixed: on a tablet, the floating mini player pill at the bottom hides while the Now Playing panel is open on the right (it comes back when you close the panel).
- Changed: tablet lyrics mode is now full screen, like a landscape canvas. Song cover + name on the left, lyrics playing on the right, nothing else on screen, and only a Back button at the bottom right.
- New: Settings > Lyrics size (Small, Medium, Large) for phone and tablet.
- New: Settings > Keep screen on in lyrics (on by default), so the screen does not sleep while you read lyrics.
- New: Library > YouTube Music > Your YouTube artists row (artists you follow, only when signed in). Tap one to open the artist page.

## step24 (v0.12.0)
- New: optional Google sign-in INSIDE the app (Settings > Account, or Library > Sign in with Google). It opens a normal Google login page in the app, no Google Cloud setup. It is never part of onboarding and VAME works fully without it.
- New: your YouTube Music playlists show in Library (row "YouTube Music") without importing anything. Tap one to open it, play it or shuffle it. Refresh button reloads them.
- New: YouTube liked songs page (first tile in the YouTube Music row).
- New: save a YouTube playlist as a normal VAME playlist with one tap (the add-to-library icon on its page).
- New: Settings > Account > Copy YouTube likes to my Liked songs (only adds, never removes).
- New: Saved albums. The bookmark icon on an album page saves it; saved albums get their own row in Library.
- New: Settings > Your name. The name shows on the Library stats cards (falls back to your Google name, then the Home title).
- New: Sign out in Settings > Account clears the login from the phone.
- Note: the Google login is unofficial (same way as Metrolist / InnerTune). It only READS your library, never likes or edits anything. It can stop working when YouTube changes. Songs still play ad-free through NewPipe.

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
