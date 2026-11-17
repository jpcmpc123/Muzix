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
- Changing the audio quality now clears the player cache and every
  stored stream format, so songs that already played re-resolve and
  play at the newly chosen quality instead of the cached old one
  (core: handler watches the quality setting).
- Home loads once on open: the five separate load triggers in
  HomeViewModel (region, language, cookie x2, mood params) were merged
  into a single combined flow, so the feed no longer shows, goes back
  to loading and shows again on startup (branch fix/home-single-load).
- MainActivity.kt: the official-build signature check is commented out
  (marked "TEST BUILD ONLY") because release builds verify the signing
  certificate and this test build is signed with a throwaway key.
