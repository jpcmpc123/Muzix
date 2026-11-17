# Test build only — branch test/all-fixes-release-apk

This branch exists only to build installable test APKs of jpcmpc123's fixes
combined. It is never merged and never part of an upstream pull request.

Contents on top of upstream `dev`:

- #2714 — bottom bar keeps the current tab selected after Back
  (AppBottomNavigationBar.kt, LiquidGlassAppBottomNavigationBar.android.kt)
- #2711 — search state survives rotation: MainActivity handles `orientation`
  in configChanges (AndroidManifest.xml) + SearchScreen safety nets
- Performance — top bars only blur while "Enable liquid glass" is on
  (LocalGlassEffectsEnabled, UIExt.kt bar modifiers, 16 screens)
- Playback (via the core submodule swap in the workflow, jpcmpc123/core
  branch fix/playback-three-layers): full-track preloader, gap watchdog and
  automatic stream-error recovery. v2: recovery also covers
  ERROR_CODE_IO_UNSPECIFIED and retries 6 times over ~1 minute. v3: the
  loader itself now rides out outages — the media source's load error policy
  retries a dropped connection for ~3 minutes with backoff before the error
  can reach the player (the Spotify/YouTube Music pattern), with HTTP
  403/404/410 failing fast so the player-level recovery can re-resolve a
  fresh URL. v4: resume-on-reconnect — a network callback in the media
  service handler resumes playback the moment the connection returns
  (seek + prepare at once) when an outage interrupted wanted playback,
  instead of waiting out the retry timers. v5: the recovery and the
  reconnect resume no longer call prepare() after the seek — on the
  crossfade player adapter, prepare() reloads the current track from 0
  with shouldPlay=false and cancels the load the seek just started, which
  is why every earlier recovery ended restarted and paused. The seek
  alone is the reload, with playWhenReady restored first. v6: the
  reconnect resume also missed the common short outage — for anything
  shorter than the loader's retry window no error ever reaches the
  handler, and the crossfade adapter keeps reporting the track as
  playing while it is stalled mid-track, so the resume's guards
  declined and the reconnect was consumed with nothing left pending.
  The adapter now exposes a live stall signal (MediaPlayerInterface
  .isStalled: the cached loading flag OR the raw player in
  STATE_BUFFERING with playWhenReady set), the resume acts on it too,
  and onAvailable grants one second chance 2s later, bound to the
  same track, for networks announced before they are usable. Also:
  propagatePlayerError no longer triggers a second, synthetic 403
  notification on top of the real error.
- Asking for a Premium quality without Premium now falls back to the
  best audio the response actually carries (itag 251) instead of the
  last format in the list (in practice ~48-70 kbps, below Low).
- Changing the audio quality now clears the player cache and every
  stored stream format, so songs that already played re-resolve and
  play at the newly chosen quality instead of the cached old one
  (core: handler watches the quality setting).
- Faster start: the Tidal BPM/key lookup (AutoMix metadata) no longer
  blocks the stream resolve — the format is stored, the URL is emitted
  and the player starts; a background lookup patches the stored row
  when the answer arrives (core: StreamRepositoryImpl).
- Faster start, no URL probe: the playback resolver no longer HEADs
  every stored stream URL before using it. Stored URLs are trusted
  until 10 minutes before their stored expiry; a URL that turns out
  dead fails the load with a 403 and the player adapter re-resolves
  it at the same position (existing machinery), and the media service
  drops all stored formats when the network returns after a loss —
  stored URLs are bound to the resolving network's IP — before the
  resume-on-reconnect reloads anything (core: Media3ServiceModule,
  MediaServiceHandlerImpl). Downloads and cast keep their probe.
- List thumbnails load at display size: row image requests cap the
  decode at 256px (small rows) or 640px (cards/grid), at least 1.25x
  the pixels the screen shows, instead of decoding the largest
  variant (540px, up to 1280x720) into 36-160dp slots; row crossfade
  drops from 550ms to 150ms (branch fix/list-image-size). Full-size
  artwork views (player, headers) are untouched.
- List thumbnails at display size, part 2 (same branch): the same
  recipe now covers the remaining small/list images — library canvas
  cards (640x1152), library tiling sleeves, podcast episode rows,
  the search result thumb, the podcast author avatar, the song
  bottom-sheet thumb, notification rows/cards and the settings
  account avatar (256px small slots, 640px cards, crossfade 150ms).
  Playlist/podcast/analytics headers, the Home hero carousel and
  the analytics mosaic stay full-size by design.
- Home loads once on open: the five separate load triggers in
  HomeViewModel (region, language, cookie x2, mood params) were merged
  into a single combined flow, so the feed no longer shows, goes back
  to loading and shows again on startup (branch fix/home-single-load).
- MainActivity.kt: the official-build signature check is commented out
  (marked "TEST BUILD ONLY") because release builds verify the signing
  certificate and this test build is signed with a throwaway key.
