# Changelog

## Widescreen Edition on AABrowser v3.0

- Rebased onto upstream kododake/AABrowser v3.0 (Material 3 Expressive UI, modular architecture).
- Floating dock rebuilt in Compose: menu, crop-to-fill, reset and zoom in/out buttons. Still follows the quick-action button position, always-visible and auto-hide settings; auto-hide delay is 10s.
- Crop-to-fill / zoom / reset video controls also available in the menu sheet.
- Page fullscreen requests are handled in-page (crop-to-fill) instead of Android's native fullscreen view.
- YouTube two-column layout fixes, rotary-knob video zoom and the iDrive-style virtual cursor carried over.
- BrowserCarAppService, `template` automotive descriptor and BROWSABLE intent category carried over.

## Unreleased

- Added a native start page with six quick-link slots.
- Added default quick links for `m.youtube.com`, Google, Twitch, Kick, Wikipedia, and Weather.com.
- Added bookmark-menu controls to pin the current page to the start page.
- Added a custom home page preference that disables the start page while active.
- Added support for multiple browser tabs with an in-app tab manager.
- Added a startup preference to restore the previous tab session when no home page is set.
- Added light theme and AMOLED dark theme options for the app UI.
- Added a beta toggle to darken supported web pages through WebView algorithmic darkening.
- Added global display scale presets and a custom scale percentage input.
- Added a startup preference to reopen the last visited page when no home page is set.
- Added an optional always-visible URL bar overlay for faster navigation.
- Added configurable floating-button behavior, visibility, and corner placement.
- Added local caching for first-party site icons on start-page cards.
- Added start-page background image selection and management from Settings.
