/*
 * Copyright (C) 2025 AABrowser Contributors (https://github.com/kododake/AABrowser)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.kododake.aabrowser.web

/**
 * JavaScript snippets for the Widescreen Edition: crop-to-fill video, video zoom,
 * player-UI hiding and the YouTube layout fixes.
 */
object WidescreenScripts {

    private fun jsArray(items: List<String>): String =
        items.joinToString(separator = ",", prefix = "[", postfix = "]") { "'$it'" }

    private val VIDEO_SELECTORS = jsArray(
        listOf(
            "video", ".html5-main-video", ".video-stream",
            "[class*=\"player\"] video", "[class*=\"video\"] video",
            ".video-player__container video", ".vp-video video",
            ".dmp_VideoView-video", ".jw-video", "#dv-web-player video"
        )
    )

    /** Player chrome that gets hidden while a video is cropped to fill the screen. */
    private val CONTROL_SELECTORS = jsArray(
        listOf(
            ".ytp-chrome-bottom", ".ytp-chrome-top", ".ytp-gradient-bottom",
            ".ytp-gradient-top", ".ytp-show-cards-title", "ytm-header-bar",
            ".ytp-ce-element", ".ytp-pause-overlay", ".video-player__overlay",
            ".vp-controls", ".vp-title", ".vp-nudge-wrapper",
            ".jw-controls", ".jw-title", ".jw-overlays", ".dmp_Controls",
            ".video-controls", ".player-controls", ".controls-container"
        )
    )

    /** Overlays hidden while the video is zoomed in. */
    private val OVERLAY_SELECTORS = jsArray(
        listOf(
            ".ytp-ce-element", ".ytp-pause-overlay", ".video-player__overlay",
            ".vp-controls", ".vp-title", ".jw-overlays", ".dmp_Controls"
        )
    )

    /** Defines `saveStyles(video)` so the original inline styles can be restored later. */
    private const val SAVE_STYLES_FN = """
        function saveStyles(v) {
            if (v.hasAttribute('data-orig-fit')) return;
            v.setAttribute('data-orig-fit', v.style.objectFit || '');
            v.setAttribute('data-orig-pos', v.style.position || '');
            v.setAttribute('data-orig-top', v.style.top || '');
            v.setAttribute('data-orig-left', v.style.left || '');
            v.setAttribute('data-orig-w', v.style.width || '');
            v.setAttribute('data-orig-h', v.style.height || '');
            v.setAttribute('data-orig-z', v.style.zIndex || '');
            v.setAttribute('data-orig-object-position', v.style.objectPosition || '');
            v.setAttribute('data-orig-transform', v.style.transform || '');
            v.setAttribute('data-orig-transition', v.style.transition || '');
            v.setAttribute('data-orig-transform-origin', v.style.transformOrigin || '');
        }
    """

    private const val LOCK_BODY_SCROLL_FN = """
        function lockBodyScroll() {
            if (!document.documentElement.hasAttribute('data-orig-overflow')) {
                document.documentElement.setAttribute('data-orig-overflow', document.documentElement.style.overflow || '');
            }
            if (!document.body.hasAttribute('data-orig-overflow')) {
                document.body.setAttribute('data-orig-overflow', document.body.style.overflow || '');
            }
            document.documentElement.style.setProperty('overflow', 'hidden', 'important');
            document.body.style.setProperty('overflow', 'hidden', 'important');
        }
    """

    /** Stretches [v] over the whole viewport with `object-fit: cover`. */
    private const val FILL_VIEWPORT_FN = """
        function fillViewport(v) {
            v.style.setProperty('position', 'fixed', 'important');
            v.style.setProperty('top', '0', 'important');
            v.style.setProperty('left', '0', 'important');
            v.style.setProperty('width', '100vw', 'important');
            v.style.setProperty('height', '100vh', 'important');
            v.style.setProperty('z-index', '2147483647', 'important');
            v.style.setProperty('object-fit', 'cover', 'important');
            v.style.setProperty('object-position', 'center center', 'important');
        }
    """

    /** Crop-to-fill for the first video found on the page (toolbar / menu button). */
    val CROP_TO_FILL_JS: String = """
        (function() {
            $SAVE_STYLES_FN
            $LOCK_BODY_SCROLL_FN
            $FILL_VIEWPORT_FN
            var selectors = $VIDEO_SELECTORS;
            var video = null;
            for (var i = 0; i < selectors.length; i++) {
                video = document.querySelector(selectors[i]);
                if (video) break;
            }
            if (!video) return;
            saveStyles(video);
            fillViewport(video);
            video.style.setProperty('transform', 'none', 'important');
            $CONTROL_SELECTORS.forEach(function(s) {
                document.querySelectorAll(s).forEach(function(el) {
                    el.style.setProperty('display', 'none', 'important');
                });
            });
            lockBodyScroll();
        })();
    """.trimIndent()

    /** Crop-to-fill for the element that just went fullscreen (`onShowCustomView`). */
    val FULLSCREEN_VIDEO_JS: String = """
        (function() {
            $SAVE_STYLES_FN
            $LOCK_BODY_SCROLL_FN
            $FILL_VIEWPORT_FN
            var fsEl = document.fullscreenElement || document.webkitFullscreenElement;
            if (!fsEl) return;
            var videos = fsEl.tagName.toLowerCase() === 'video' ? [fsEl] : fsEl.querySelectorAll('video');
            for (var i = 0; i < videos.length; i++) {
                saveStyles(videos[i]);
                fillViewport(videos[i]);
            }
            $CONTROL_SELECTORS.forEach(function(s) {
                document.querySelectorAll(s).forEach(function(el) {
                    el.style.setProperty('display', 'none', 'important');
                });
            });
            lockBodyScroll();
        })();
    """.trimIndent()

    /** Scales the first video found on the page around its centre. */
    fun zoomVideoJs(scale: Double): String = """
        (function() {
            $SAVE_STYLES_FN
            var selectors = $VIDEO_SELECTORS;
            var video = null;
            for (var i = 0; i < selectors.length; i++) {
                video = document.querySelector(selectors[i]);
                if (video) break;
            }
            if (!video) return;
            saveStyles(video);
            video.style.transform = 'scale($scale)';
            video.style.transition = 'transform 0.1s ease-out';
            video.style.transformOrigin = 'center center';
            if ($scale > 1.0) {
                $OVERLAY_SELECTORS.forEach(function(s) {
                    document.querySelectorAll(s).forEach(function(el) {
                        el.style.setProperty('display', 'none', 'important');
                    });
                });
            }
        })();
    """.trimIndent()

    /** Defines `window.restoreUI()`, which undoes crop-to-fill / zoom. Injected on every page load. */
    val RESTORE_UI_JS: String = """
        window.restoreUI = function() {
            $CONTROL_SELECTORS.forEach(function(s) {
                document.querySelectorAll(s).forEach(function(el) { el.style.display = ''; });
            });

            if (document.documentElement.hasAttribute('data-orig-overflow')) {
                document.documentElement.style.overflow = document.documentElement.getAttribute('data-orig-overflow');
                document.documentElement.removeAttribute('data-orig-overflow');
            }
            if (document.body.hasAttribute('data-orig-overflow')) {
                document.body.style.overflow = document.body.getAttribute('data-orig-overflow');
                document.body.removeAttribute('data-orig-overflow');
            }

            var props = [
                ['data-orig-fit', 'objectFit'], ['data-orig-pos', 'position'],
                ['data-orig-top', 'top'], ['data-orig-left', 'left'],
                ['data-orig-w', 'width'], ['data-orig-h', 'height'],
                ['data-orig-z', 'zIndex'], ['data-orig-object-position', 'objectPosition'],
                ['data-orig-transform', 'transform'], ['data-orig-transition', 'transition'],
                ['data-orig-transform-origin', 'transformOrigin']
            ];
            var cssNames = [
                'position', 'top', 'left', 'width', 'height', 'z-index', 'object-fit',
                'object-position', 'transform', 'transition', 'transform-origin'
            ];
            var videos = document.getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                if (v.hasAttribute('data-orig-fit')) {
                    props.forEach(function(p) {
                        v.style[p[1]] = v.getAttribute(p[0]);
                        v.removeAttribute(p[0]);
                    });
                } else {
                    cssNames.forEach(function(n) { v.style.removeProperty(n); });
                }
            }
            window.dispatchEvent(new Event('resize'));
        };
    """.trimIndent()

    const val CALL_RESTORE_UI_JS = "if (typeof restoreUI === 'function') { restoreUI(); }"

    /** Runs at page start on youtube.com so the first paint already uses the wide layout. */
    val YOUTUBE_PREEMPTIVE_JS: String = """
        (function() {
            var css = 'ytd-watch-flexy[flexy] #primary.ytd-watch-flexy { margin-left: 0 !important; margin-right: auto !important; } ytd-watch-flexy[flexy] #secondary.ytd-watch-flexy { display: block !important; }';
            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = css;
            (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()

    /** Forces YouTube's two-column desktop layout and keeps it across SPA navigations. */
    val YOUTUBE_LAYOUT_FIX_JS: String = """
        (function() {
            function applyFix() {
                var watchPage = document.querySelector('ytd-watch-flexy');
                if (watchPage) {
                    watchPage.setAttribute('is-two-columns-layout', '');
                    watchPage.removeAttribute('theater');
                    watchPage.removeAttribute('fullscreen');
                    watchPage.removeAttribute('is-extra-wide-video');

                    var playerData = watchPage.querySelector('#player-container-outer');
                    if (playerData) {
                        playerData.style.marginLeft = '0';
                        playerData.style.marginRight = '0';
                    }
                }
                var cinematic = document.querySelector('#cinematic-container');
                if (cinematic) cinematic.style.display = 'none';
                window.dispatchEvent(new Event('resize'));
            }
            applyFix();
            document.addEventListener('yt-navigate-finish', applyFix);
            var observer = new MutationObserver(function() {
                var page = document.querySelector('ytd-watch-flexy');
                if (page && !page.hasAttribute('is-two-columns-layout')) {
                    applyFix();
                }
            });
            observer.observe(document.documentElement, { attributes: true, childList: true, subtree: true });
        })();
    """.trimIndent()
}
