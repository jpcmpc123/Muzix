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
  instead of waiting out the retry timers.
- MainActivity.kt: the official-build signature check is commented out
  (marked "TEST BUILD ONLY") because release builds verify the signing
  certificate and this test build is signed with a throwaway key.
