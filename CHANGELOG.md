# VAME Music - full changelog (up to zip step52, v0.28.0, 27 Oct 2026)

## step52 (v0.28.0) - includes everything up to step51
- New: sign in a TV from your phone, no typing on the TV. On the TV open Settings > Account > Sign in with Google: it now shows a 6-digit code at the top. On the phone (already signed in, same Wi-Fi) open Settings > Account > Sign in a TV, type the code and tap Send sign-in to <TV name>. The TV signs in by itself.
- New: after 5 wrong codes the TV makes a new code. The code stops working as soon as the page is closed or the sign-in is done.
- The normal web sign-in (with the Keyboard / Next buttons) is still there as a backup.
- Needs: both devices on the same Wi-Fi, VAME open on both, the phone signed in first.
- Phone and tablet: nothing else changes.

## step51 (v0.27.2) - includes everything up to step50
- Fixed: on TV, moving the remote down in Settings could land on the name / title text box, which opened the TV keyboard and trapped the remote. Now those boxes are plain buttons on TV: press OK to type, Done (or Back) to finish. The keyboard never opens by itself in Settings.
- Phone and tablet: no change.

## step50 (v0.27.1) - includes everything up to step49
- Fixed: signing in on TV, the keyboard would not go away after typing the email. It no longer opens in a forced way, so the remote's Back key closes it.
- New: three buttons on top of the TV sign-in page: Keyboard (open), Hide keyboard, and Next (presses the page's Next / Sign in button for you, then closes the keyboard). Use them for the email and again for the password.
- Phone and tablet: no change.

## step49 (v0.27.0) - includes everything up to step48
- New: TV left bar is back. VAME logo, Home / Explore / Your type / Library / Settings, and at the bottom the song that is playing now (cover, name, artist, play / pause badge). Press OK on it and the player opens. The pill at the bottom is gone on TV.
- New: TV player is now three parts: left = canvas (big cover with a very slow zoom), middle = controls (title, artist, seek bar, previous / play / next, shuffle, repeat, like, lyrics, play on another device, and a big Queue button), right = the queue.
- New: the queue has an X button on top. Press it (or Back) and the queue closes, the player becomes two parts (canvas + controls). The Queue button is always there to open it again. When the queue opens the remote goes to the X, when it closes it goes back to the Queue button.
- New: TV queue shows the whole list with the current song marked, OK on a song plays it, a shuffle button on top. The middle part also shows a "Next: ..." line.
- Fixed: the genre page ("What type of songs do you like?") now works with the remote. It shows 4 genres per row on TV, the first one is selected at start, Down goes to Continue.
- Fixed: Settings now works with the remote (up / down / left / right / OK). The first button is selected when it opens, every chip, switch, button and row gets a white ring, and the back arrow is a big button.
- New: colour sliders in Settings > Look work with the remote (Left / Right change the value).
- Fixed: signing in to YouTube on TV: the keyboard opens by itself when the cursor is on a text field, the first field is selected when the page loads, and there is a Keyboard button on top in case it does not.
- Phone and tablet: no visible change.

## step48 (v0.26.1) - includes everything up to step47
- Fixed: the TV remote did not move anything on the "What type of songs do you like?" page and other full pages. Cause: every full page had an invisible "catch all touches" layer that the remote landed on, and the TV Home underneath also stayed selectable. Now the remote only reaches what is on top: first-launch genres, Settings, Recap, Sign in, album / artist / playlist / liked pages, the player and the lyrics.
- Fixed: on TV the pages underneath are removed while one of those is open (Home keeps its scroll place and comes back when you press Back).
- Fixed: the tabs on top close an open album / artist page before they switch.
- Fixed: the pill is hidden behind the first-launch genres page, Recap and Sign in so the remote cannot land on it there.
- The TV player now keeps trying to select Play for a few seconds after it opens.
- Phone and tablet: no visible change (the touch blocking works the same).

## step47 (v0.26.0) - includes everything up to step46
- Changed: the TV look now follows the Spotify TV app. The left menu is gone. On top: VAME on the left, the tabs Home / Explore / Your type / Library in the middle, "Welcome name" (opens Settings) on the right.
- New: TV Home has a big picture at the top right with the name of whatever the remote is on, fading into the page. It changes as you move over songs and albums.
- New: under it a grid of 8 wide tiles (small cover + name, 4 per row) from your recents, likes and picks, then rows of cards (YouTube Music shelves, Liked songs / Made for you, Albums, Quick picks, genres, Your artists).
- Changed: the picture and big name fade away when you move down to the lower rows.
- The pill at the bottom stays as it was.

## step46 (v0.25.0) - includes everything up to step45
- Fixed: the TV remote (D-pad) now works. VAME takes the remote at start and, if nothing is selected yet, the first arrow / OK press selects Home. Every TV button now has its own white ring and grows a little while the remote is on it.
- Changed: the left menu on TV is slim (icons with small names) and no longer takes a big part of the screen.
- Changed: everything on TV is a bit smaller, so more fits on the screen.
- New: TV Home like the Spotify TV app: a greeting, then rows of small cover cards (Recents, YouTube Music shelves, Liked songs, Albums, Quick picks, genres, Your artists). The card the remote is on grows and gets a white ring.
- New: on TV the pill is always at the bottom, centred, also over Settings (previous, play / pause, next; OK on the song opens the full player). With no song it says "Pick a song to start playing". It hides only behind the full screen player and the lyrics.
- Phone and tablet look is unchanged.

## step45 (v0.24.0) - includes everything up to step44
- New: VAME now runs on Android TV. It is the SAME app and the SAME file as the phone / tablet one: one repo, one zip, one build, one APK in Releases (VAME-0.24.0.apk). Install it on a TV and it shows in the TV app row with a VAME banner; on your phone and tablet nothing changes.
- New: TV mode switches on by itself on an Android TV. Settings > Interface > TV mode: Auto / On / Off. Choose On on your tablet to see the TV look (you can tap everything there too).
- New: TV layout. Menu on the left (Home, Explore, Your type, Library, Settings) with names, big text, the same Home shelves, Explore, Library, album, artist and playlist pages.
- New: white focus ring on whatever the remote is pointing at, on every button, card and setting.
- New: TV player. Press OK on the bar at the bottom: big cover, title, shuffle, previous, play / pause, next, repeat, like, lyrics, play on another device. Left / Right on the progress bar jumps 10 seconds. The TV stays awake while it is open. Back closes it.
- New: now playing bar at the bottom of every TV page (previous, play / pause, next).
- New: remote media keys work: play / pause, next, previous, fast forward and rewind (10 seconds).
- New: Settings switches can be turned on / off with the remote (OK on the whole row).
- New: your TV also shows up as a device in "Play on another device" on your phone (Connect), so you can send a song from the phone to the TV.
- Note: not tested on a real TV yet. Sign in with Google and the Spotify page need a keyboard / browser and are not made for the TV remote.

## step44 (v0.23.0) - includes everything up to step43
- New: Lyrics screen redesigned like your recording. Top: back, "LYRICS" with song and artist, sleep timer button, menu button. Under it a small label (SYNCED or PLAIN TEXT + the provider name). Tap the label to change provider.
- New: Lyrics provider window (tool button 1 or the label): shows the current provider, "Try another source" (skips the current one and searches the others) and a list: Auto, LRCLIB, NetEase. Also in Settings > Songs and lyrics > Lyrics source.
- New: NetEase as a second lyrics source (good for Hindi / Punjabi songs). Auto tries LRCLIB first, then NetEase.
- New: Translate button (one tap on / off, no language list on the lyrics page). It puts a translated line under every lyric line. The language is chosen in Settings > Songs and lyrics: "Hindi songs translate to" (default English) and "Other songs translate to" (default Hindi), each with Off / English / Hindi / Spanish / French. The song language is detected by itself (Devanagari letters, else Google detection). Uses Google Translate's public web address, so it needs internet and may stop working if Google changes it.
- New: Globe button turns Hinglish (Hindi in English letters) on / off right from the lyrics screen.
- New: Share button sends the song name and the lyrics as text to any app.
- New: Text size button opens a slider (50 to 160 percent) with a live number and Reset. The Small / Medium / Large chips in Settings still work (80 / 100 / 120 percent).
- New: Fullscreen button hides all the buttons and shows only the lyrics. A small button at the top right brings them back (Back also does).
- New: Wave seek bar on the lyrics screen: three soft waves that move while music plays, a dot, times underneath. Tap or drag to seek.
- New: Previous / Play-Pause / Next round buttons on the lyrics screen.
- Better: the line that is being sung glows, and its words fill by word length over the time the line is really sung (not over the silent gap after it). Hidden word-time tags in some lyric files no longer show as text.
- Better: tablet / sideways lyrics get the same tool buttons under the lyrics (when the screen is tall enough).
- Note: real word-by-word timing (like Apple Music / QQ Karaoke) is NOT available from LRCLIB or NetEase, so the word fill is estimated from the line time. Apple Music, QQ Karaoke, BetterLyrics, LastWave and lrc.red from your video are not added (they need keys, accounts or unstable private servers).

## step43 (v0.22.0) - includes everything up to step42
- New: home screen widget 4x1 - cover on the left, song and artist in the middle, previous / play-pause / next on the right. Tap the widget to open VAME.
- New: home screen widget 2x2 - big rounded cover with the song and artist on it and a small play / pause button. Tap to open VAME.
- New: both widgets follow the song and play / pause by themselves. They are in the widget list as "VAME 4x1" and "VAME 2x2".
- New: Settings > Player > "Lyrics blur": Blur (as before), Light blur, No blur.
- Fixed: Settings on / off switches looked all white. ON is now green with a white knob, OFF is dark grey with a grey knob and a clear border.

## step42 (v0.21.2) - includes everything up to step41
- Removed: sending VAME playlists to Spotify (the "Copy" and "Merge" buttons and the "Your VAME playlists" card on the Spotify page). Listening to Spotify inside VAME and "To VAME" (bringing a Spotify playlist into VAME) stay. Texts in Library and Settings updated.

## step41 (v0.21.1) - includes everything up to step40
- Removed: "Move to another app" (the Library tile, the Settings entry and its screen). Music can no longer be copied out to Spotify / YouTube Music / a file from there. Import into VAME and the Spotify screen stay.

## step40 (v0.21.0) - includes everything up to step39
- New: Play on another device (like Spotify Connect). Open the output button on Now Playing (the Bluetooth / phone icon): a new list "Other VAME devices (same Wi-Fi)" shows your other phone / tablet running VAME. Tap one and the song moves there and keeps its place.
- New: while the music plays on the other device, a small card at the top shows the song, Previous / Play-Pause / Next, volume - and +, and a "This device" button that brings the music back here.
- New: change the song from your phone (tap any song, Next, Previous) and it changes on the other device. The seek bar and lyrics follow the other device too.
- New: when the other device moves to its next song by itself, your phone follows it.
- New: Settings > Standby: "Let other devices play here" switch (turn it off to stop others from controlling this device).
- Note: no account and no internet server needed, only the same Wi-Fi. TV app comes next (step41), it will use this same feature.

## step39 (v0.20.3) - includes everything up to step38
- New: Standby button on the tablet (left rail, under Library). It opens the same big Standby player as the phone sideways (cover left, clock, controls right). The side player and the rail are NOT removed: they stay under it and come back when you close Standby (Back, tap the grabber at the top, or swipe it down).
- New: in tablet Standby, tapping the lyrics line or the lyrics button opens the normal tablet lyrics (cover + name left, lyrics right). Back returns to Standby.
- New: Settings > Standby: "Standby button on tablet" switch (hide or show the button).
- New: Settings > Standby: "12-hour clock in Standby" switch.
- Standby switches off by itself if the tablet is turned upright.

## step38 (v0.20.2) - includes everything up to step37
- Fixed: on the tablet the album / YouTube playlist page had a huge cover that filled the whole screen, so the list looked stuck and would not scroll. The cover is now small (max 240dp).
- New: every song on album and YouTube playlist pages shows a small cover (48dp). If a song has no cover, the playlist cover is used.
- New: on the tablet the album page header is side by side (cover left, title and buttons right).
- New: album page shows "N songs - X min" under the artist name.
- New: Liked songs page also shows the total minutes.
- New: round "back to top" button appears on album, playlist and liked pages after scrolling down.

## step37 (v0.20.1) - includes everything up to step36
- Fixed: the player animation was too fast to see on the phone and not smooth on the tablet. It now takes about half a second with a soft landing. On the phone the whole player screen melts onto the mini player pill while the big cover flies down onto the pill cover. On the tablet the side player shrinks into the small box or pill and grows back out.
- Fixed: both animations are drawn once and then only moved, so they no longer stutter while the player is heavy.
- New: Settings, Lyrics, Recap, Sign in, Spotify and Move to another app use the same soft half-second grow and shrink.
- New: greeting under the Home title (Good morning / afternoon / evening, with your name).
- New: Home titles and shelves rise in softly as you scroll to them.
- New: the Recents grid opens and closes with a smooth animation.
- New: soft vibration on the tablet box buttons (previous, play, next).

## step36 (v0.20.0) - includes everything up to step35
- Changed: Home is now all big cover cards with the name under the picture. No more small-picture rows with a wide name. Recommended Albums, Suggested for you and the Music / Podcasts tabs are cards or grids now.
- New: Recents shelf at the top of Home. The grid button next to the title opens all your recent songs as a grid.
- New: YouTube Music shelves on Home (Listen again, Quick picks, Featured playlists, Covers and remixes, Trending, India's biggest hits and more). Signed in = your own shelves, signed out = the general ones. Tap a song to play it, an album or playlist to open its page, an artist to open the artist page. Settings > Home > YouTube Music shelves turns them off.
- New: shelf titles are bigger with a small dim line under them, cards show "Song / Album . Artist", and the song that is playing shows its name in the accent colour. About two cards and a piece of the third show on a phone.
- New: the player is sucked into the mini player pill when you close it, and pops out of it when you open it. The screen squeezes flat towards the pill and the big cover flies down onto the pill cover.
- New: on a sideways tablet the side player grows out of the small box (or pill) and shrinks back into it.
- New: album, artist, playlist, liked and downloads pages grow out of the card you tapped and shrink back into it.
- New: loading shimmer (soft moving grey cards) on Home, Explore and Podcasts while songs load.
- New: nicer empty and error screens (no playlists, no songs, nothing downloaded, no liked songs, YouTube Music shelves failed with a Try again button).
- New: soft vibration on taps (cards, chips, bottom bar, mini player). Settings > Interface > Vibration on tap.
- New: the colour of the playing cover tints the top of every page. Settings > Interface > Cover colour on pages.
- Changed: Settings > Home "Last played albums" is now "Recents".
- NOT built yet (asked for, left for later): update download as a notification that keeps going with the app closed, and multi-column tablet grids on Explore and Library (Home already uses bigger cards on tablets).

## step35 (v0.19.0) - includes everything up to step34
- New: Standby mode for phones. Turn the phone sideways while music plays and a big landscape player opens by itself: cover on the left, title, progress bar, rewind / play / forward, volume, lyrics, sound output, repeat and queue on the right, plus a small clock. Turn the phone back and it closes.
- New: Standby lyrics. Tap the lyrics button (or the lyric line) in Standby: cover and song name on the left, synced lyrics on the right, back button bottom right. Settings > Player > Standby opens with: Player or Lyrics.
- New: Standby can keep the screen on and hide the status bar (both have a switch).
- Changed: a phone held sideways no longer gets the tablet side rail. Only screens with a short side of 600dp or more count as tablets.
- New: the tablet small box is bigger (156dp), shows the song name and has previous / play / next buttons.
- New: Settings > Player > Tablet small player: Box or Pill. The pill is a medium bar (not full width) in the middle of the bottom. Pill width: Compact, Medium, Wide.
- New: the music (pill or box) stays on top of Settings so you can pause or skip while changing something. Tap it to leave Settings and open the player.
- New: Settings has a new look: one glass card per group with a round icon, wider cards on tablets, nicer switches.
- New: more customisation: Interface size (Small / Normal / Large), Glass strength, bottom bar names on/off, Calmer animations, and Home shelves on/off (Last played albums, Quick picks, Genre shelves, Your artists).
- New: Spotify page (Library > Spotify, or Settings > Spotify). Connect with the official Spotify login (one-time free app on developer.spotify.com, paste its Client ID). Listen inside VAME with Spotify's web player, copy a VAME playlist to a new Spotify playlist, merge a VAME playlist into an existing Spotify playlist, copy a Spotify playlist into VAME.
- New: Move to another app (Library or Settings). Moves playlists, liked songs, followed artists and saved albums straight to Spotify, playlists and liked songs to YouTube Music, or saves a CSV list (for other apps and transfer websites) and a VAME backup file. A backup file can be restored.
- Note: Spotify rules (2026): the app owner needs Premium and only accounts added in the dashboard can connect. The web player needs Premium and may not play on every phone (protected audio), there is a button to open the Spotify app instead.

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
