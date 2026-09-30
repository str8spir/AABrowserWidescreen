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

package com.kododake.aabrowser.ui.controllers

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.kododake.aabrowser.data.BrowserPreferences
import com.kododake.aabrowser.databinding.ActivityMainBinding
import com.kododake.aabrowser.main.BrowserManagersProvider
import com.kododake.aabrowser.tabs.BrowserTab
import com.kododake.aabrowser.web.WidescreenScripts

/**
 * Widescreen Edition extras:
 *  - crop-to-fill / zoom / reset for page video (dock, menu and rotary knob)
 *  - car controller support (iDrive rotary, nudges, push, touchpad) with an on-screen cursor
 */
class WidescreenController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val provider: BrowserManagersProvider
) {
    private val handler = Handler(Looper.getMainLooper())

    private var videoZoomScale = MIN_ZOOM

    private var cursorX = 0f
    private var cursorY = 0f
    private var isCursorVisible = false
    private val hideCursorRunnable = Runnable { hideVirtualCursor() }

    // ---- Video crop-to-fill / zoom -------------------------------------------------------

    fun applyCropToFill() {
        isVideoModeActive = true
        provider.uiManager.setImmersiveMode(true)
        provider.tabManager.activeTab?.webView?.evaluateJavascript(WidescreenScripts.CROP_TO_FILL_JS, null)
        provider.uiManager.showMenuButtonTemporarily()
    }

    fun zoomVideo(delta: Double) {
        videoZoomScale = (Math.round((videoZoomScale + delta) * 100.0) / 100.0).coerceIn(MIN_ZOOM, MAX_ZOOM)
        provider.tabManager.activeTab?.webView
            ?.evaluateJavascript(WidescreenScripts.zoomVideoJs(videoZoomScale), null)
        provider.uiManager.showMenuButtonTemporarily()
    }

    fun zoomIn() = zoomVideo(ZOOM_STEP)

    fun zoomOut() = zoomVideo(-ZOOM_STEP)

    /**
     * Undoes crop-to-fill / zoom in [tab] (defaults to the active tab). When the tab is the
     * active one, system bars go back to whatever the "Fullscreen" menu switch says.
     */
    fun resetZoom(tab: BrowserTab? = provider.tabManager.activeTab) {
        if (tab != null && tab == provider.tabManager.activeTab) {
            videoZoomScale = MIN_ZOOM
            isVideoModeActive = false
            provider.uiManager.setImmersiveMode(BrowserPreferences.shouldUseFullscreenMode(activity))
        }
        tab?.webView?.evaluateJavascript(WidescreenScripts.CALL_RESTORE_UI_JS, null)
    }

    // ---- Car controller (iDrive rotary / nudges / push / touchpad) -----------------------
    //
    // Events are taken at the Activity's dispatch level (see MainActivity), before any view
    // sees them, so they work no matter what has focus (WebView, Compose views or nothing).
    //
    //   nudge up/down/left/right (DPAD)   -> move the on-screen cursor
    //   push (DPAD_CENTER / ENTER)        -> click at the cursor if it is showing,
    //                                        otherwise play/pause the page's video
    //   rotate (rotary scroll / NAVIGATE) -> zoom while a video is cropped or zoomed,
    //                                        otherwise scroll the page
    //   media play/pause keys             -> play/pause the page's video
    //   touchpad / mouse movement         -> move the cursor

    private var isVideoModeActive = false

    /** @return true when the key event was consumed. */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        inputMonitor?.log(event)
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false
        // Leave keys alone while the menu / address bar is open so normal focus and typing work.
        if (binding.menuOverlay.isVisible) return false

        val isDown = event.action == KeyEvent.ACTION_DOWN
        val firstDown = isDown && event.repeatCount == 0
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { if (isDown) moveCursorBy(0f, -nudgeStep(event)); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (isDown) moveCursorBy(0f, nudgeStep(event)); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (isDown) moveCursorBy(-nudgeStep(event), 0f); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { if (isDown) moveCursorBy(nudgeStep(event), 0f); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (firstDown) {
                    if (isCursorVisible) dispatchClickAtCursor() else toggleVideoPlayback()
                }
                return true
            }
            KeyEvent.KEYCODE_NAVIGATE_NEXT -> { if (isDown) onRotate(1f); return true }
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> { if (isDown) onRotate(-1f); return true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (firstDown) toggleVideoPlayback(); return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> { if (firstDown) runVideoJs(WidescreenScripts.PLAY_VIDEO_JS); return true }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { if (firstDown) runVideoJs(WidescreenScripts.PAUSE_VIDEO_JS); return true }
        }
        return false
    }

    /** @return true when the motion event was consumed. */
    fun handleGenericMotion(event: MotionEvent): Boolean {
        inputMonitor?.log(event)
        if (event.action == MotionEvent.ACTION_SCROLL &&
            (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER) || !event.isFromSource(InputDevice.SOURCE_CLASS_POINTER))
        ) {
            // Rotary knobs report AXIS_SCROLL; some hosts report them as VSCROLL instead.
            var delta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (delta == 0f) delta = -event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (delta != 0f) {
                onRotate(delta)
                return true
            }
        }

        if (event.isFromSource(InputDevice.SOURCE_TOUCHPAD) || event.isFromSource(InputDevice.SOURCE_MOUSE) ||
            event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)
        ) {
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                    // AXIS_X / AXIS_Y are absolute positions on mice and most touchpads; use the
                    // relative axes when the device reports them so the cursor moves by deltas.
                    val relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                    val relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                    if (relX != 0f || relY != 0f) {
                        moveCursorBy(relX * CURSOR_SENSITIVITY, relY * CURSOR_SENSITIVITY)
                    } else if (event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
                        moveCursorBy(event.x * CURSOR_SENSITIVITY, event.y * CURSOR_SENSITIVITY)
                    } else {
                        moveCursorTo(event.x, event.y)
                    }
                    return true
                }
            }
        }
        return false
    }

    /** Touch events are only logged (the monitor shows whether the touchpad arrives as touches). */
    fun onTouchEvent(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) inputMonitor?.log(event)
    }

    private fun onRotate(delta: Float) {
        if (isVideoModeActive || videoZoomScale > MIN_ZOOM) {
            if (delta > 0) zoomIn() else zoomOut()
        } else {
            scrollPage(delta)
        }
    }

    private fun nudgeStep(event: KeyEvent): Float {
        val base = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, NUDGE_STEP_DP, activity.resources.displayMetrics)
        // Accelerate while a nudge is held.
        return base * (1f + (event.repeatCount.coerceAtMost(8) / 2f))
    }

    /** Scrolls whatever is under the cursor (or the screen centre) with a synthetic mouse-wheel event. */
    private fun scrollPage(delta: Float) {
        val webView = provider.tabManager.activeTab?.webView ?: return
        val x = if (isCursorVisible) cursorX else webView.width / 2f
        val y = if (isCursorVisible) cursorY else webView.height / 2f
        val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, -delta * SCROLL_WHEEL_TICKS)
        })
        val now = SystemClock.uptimeMillis()
        val wheel = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0
        )
        webView.dispatchGenericMotionEvent(wheel)
        wheel.recycle()
    }

    private fun toggleVideoPlayback() {
        runVideoJs(WidescreenScripts.TOGGLE_PLAYBACK_JS)
        provider.uiManager.showMenuButtonTemporarily()
    }

    private fun runVideoJs(js: String) {
        provider.tabManager.activeTab?.webView?.evaluateJavascript(js, null)
    }

    fun cleanup() {
        handler.removeCallbacks(hideCursorRunnable)
        inputMonitor?.cleanup()
    }

    private fun moveCursorBy(dx: Float, dy: Float) = moveCursorTo(cursorX + dx, cursorY + dy, showFirst = true)

    private fun moveCursorTo(x: Float, y: Float, showFirst: Boolean = false) {
        val wasHidden = !isCursorVisible
        showVirtualCursor()
        // First nudge only reveals the cursor at the screen centre.
        if (showFirst && wasHidden) {
            positionCursor()
        } else {
            cursorX = x.coerceIn(0f, binding.root.width.toFloat().coerceAtLeast(0f))
            cursorY = y.coerceIn(0f, binding.root.height.toFloat().coerceAtLeast(0f))
            positionCursor()
        }
        provider.uiManager.showMenuButtonTemporarily()
        handler.removeCallbacks(hideCursorRunnable)
        handler.postDelayed(hideCursorRunnable, CURSOR_HIDE_DELAY_MS)
    }

    /** The cursor drawable's tip sits a little inside its top-left corner; place the tip on (cursorX, cursorY). */
    private fun positionCursor() {
        val density = activity.resources.displayMetrics.density
        binding.virtualCursor.x = cursorX - CURSOR_TIP_X_DP * density
        binding.virtualCursor.y = cursorY - CURSOR_TIP_Y_DP * density
    }

    private fun showVirtualCursor() {
        if (!isCursorVisible) {
            isCursorVisible = true
            binding.virtualCursor.isVisible = true
            binding.virtualCursor.bringToFront()
            if (cursorX == 0f && cursorY == 0f) {
                cursorX = binding.root.width / 2f
                cursorY = binding.root.height / 2f
            }
        }
    }

    private fun hideVirtualCursor() {
        isCursorVisible = false
        binding.virtualCursor.isVisible = false
    }

    private fun dispatchClickAtCursor() {
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        down.source = InputDevice.SOURCE_TOUCHSCREEN
        up.source = InputDevice.SOURCE_TOUCHSCREEN
        binding.root.dispatchTouchEvent(down)
        binding.root.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
        handler.removeCallbacks(hideCursorRunnable)
        handler.postDelayed(hideCursorRunnable, CURSOR_HIDE_DELAY_MS)
    }

    /** On-screen log of incoming input events, toggled from the menu ("Input log"). */
    private val inputMonitor: InputMonitor? = InputMonitor(binding.inputMonitor).apply {
        isEnabled = BrowserPreferences.isInputLogEnabled(activity)
    }

    val isInputLogEnabled: Boolean get() = inputMonitor?.isEnabled == true

    fun setInputLogEnabled(enabled: Boolean) {
        BrowserPreferences.setInputLogEnabled(activity, enabled)
        inputMonitor?.isEnabled = enabled
    }

    private companion object {
        const val MIN_ZOOM = 1.0
        const val MAX_ZOOM = 2.0
        const val ZOOM_STEP = 0.05
        const val CURSOR_SENSITIVITY = 1.5f
        const val CURSOR_HIDE_DELAY_MS = 5000L
        const val NUDGE_STEP_DP = 40f
        const val SCROLL_WHEEL_TICKS = 3f
        const val CURSOR_TIP_X_DP = 7f
        const val CURSOR_TIP_Y_DP = 2f
    }
}
