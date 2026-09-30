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
 *  - an on-screen virtual cursor driven by touchpad / mouse / iDrive-style controllers
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
            provider.uiManager.setImmersiveMode(BrowserPreferences.shouldUseFullscreenMode(activity))
        }
        tab?.webView?.evaluateJavascript(WidescreenScripts.CALL_RESTORE_UI_JS, null)
    }

    // ---- Rotary knob + virtual cursor ----------------------------------------------------

    /** @return true when the event was consumed. */
    fun handleGenericMotion(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER) && event.action == MotionEvent.ACTION_SCROLL) {
            val scrollDelta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (scrollDelta > 0) zoomIn() else if (scrollDelta < 0) zoomOut()
            return true
        }

        if (event.isFromSource(InputDevice.SOURCE_TOUCHPAD) || event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            when (event.action) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                    updateCursorPosition(
                        event.getAxisValue(MotionEvent.AXIS_X),
                        event.getAxisValue(MotionEvent.AXIS_Y)
                    )
                    return true
                }
            }
        }
        return false
    }

    /** @return true when the key press was turned into a click at the cursor. */
    fun handleKeyDown(keyCode: Int): Boolean {
        if (isCursorVisible && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
            dispatchClickAtCursor()
            return true
        }
        return false
    }

    fun cleanup() {
        handler.removeCallbacks(hideCursorRunnable)
    }

    private fun updateCursorPosition(dx: Float, dy: Float) {
        showVirtualCursor()
        val cursor = binding.virtualCursor
        val fallback = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, activity.resources.displayMetrics)
        val cursorWidth = if (cursor.width > 0) cursor.width.toFloat() else fallback
        val cursorHeight = if (cursor.height > 0) cursor.height.toFloat() else fallback

        cursorX = (cursorX + dx * CURSOR_SENSITIVITY).coerceIn(0f, (binding.root.width - cursorWidth).coerceAtLeast(0f))
        cursorY = (cursorY + dy * CURSOR_SENSITIVITY).coerceIn(0f, (binding.root.height - cursorHeight).coerceAtLeast(0f))

        cursor.x = cursorX
        cursor.y = cursorY

        handler.removeCallbacks(hideCursorRunnable)
        handler.postDelayed(hideCursorRunnable, CURSOR_HIDE_DELAY_MS)
    }

    private fun showVirtualCursor() {
        if (!isCursorVisible) {
            isCursorVisible = true
            binding.virtualCursor.isVisible = true
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
        binding.root.dispatchTouchEvent(down)
        binding.root.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
        provider.uiManager.showMenuButtonTemporarily()
    }

    private companion object {
        const val MIN_ZOOM = 1.0
        const val MAX_ZOOM = 2.0
        const val ZOOM_STEP = 0.05
        const val CURSOR_SENSITIVITY = 1.5f
        const val CURSOR_HIDE_DELAY_MS = 5000L
    }
}
