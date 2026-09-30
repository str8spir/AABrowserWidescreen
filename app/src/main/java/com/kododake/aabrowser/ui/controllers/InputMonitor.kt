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
 */package com.kododake.aabrowser.ui.controllers

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.view.isVisible

/**
 * Debug-build overlay listing the last few input events the app received, with their source
 * and device. Used to find out what a car's controller (e.g. BMW iDrive) actually sends
 * through Android Auto. Also logged to logcat under the "AAB_INPUT" tag.
 */
class InputMonitor(private val view: TextView) {
    private val handler = Handler(Looper.getMainLooper())
    private val lines = ArrayDeque<String>()
    private val hide = Runnable { view.isVisible = false }

    fun log(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return
        append("KEY ${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode}) src=${sourceName(event.source)} dev=${event.device?.name ?: "?"}")
    }

    fun log(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_HOVER_MOVE || event.actionMasked == MotionEvent.ACTION_MOVE) {
            // Movement is noisy: keep only one line per burst.
            if (lines.lastOrNull()?.startsWith("MOVE") == true) lines.removeLast()
        }
        val action = MotionEvent.actionToString(event.actionMasked).removePrefix("ACTION_")
        val axes = buildList {
            listOf(
                MotionEvent.AXIS_SCROLL to "scroll", MotionEvent.AXIS_VSCROLL to "vscroll",
                MotionEvent.AXIS_HSCROLL to "hscroll", MotionEvent.AXIS_RELATIVE_X to "relX",
                MotionEvent.AXIS_RELATIVE_Y to "relY"
            ).forEach { (axis, name) ->
                val v = event.getAxisValue(axis)
                if (v != 0f) add("$name=%.2f".format(v))
            }
        }.joinToString(" ")
        val prefix = if (action.contains("MOVE")) "MOVE" else "MOTION"
        append("$prefix $action x=%.0f y=%.0f $axes src=${sourceName(event.source)} dev=${event.device?.name ?: "?"}".format(event.x, event.y))
    }

    fun cleanup() = handler.removeCallbacks(hide)

    private fun append(line: String) {
        Log.d(TAG, line)
        lines.addLast(line)
        while (lines.size > MAX_LINES) lines.removeFirst()
        view.text = lines.joinToString("\n")
        view.isVisible = true
        view.bringToFront()
        handler.removeCallbacks(hide)
        handler.postDelayed(hide, HIDE_AFTER_MS)
    }

    private fun sourceName(source: Int): String {
        val names = listOf(
            InputDevice.SOURCE_ROTARY_ENCODER to "ROTARY", InputDevice.SOURCE_DPAD to "DPAD",
            InputDevice.SOURCE_KEYBOARD to "KEYBOARD", InputDevice.SOURCE_TOUCHSCREEN to "TOUCHSCREEN",
            InputDevice.SOURCE_TOUCHPAD to "TOUCHPAD", InputDevice.SOURCE_MOUSE to "MOUSE",
            InputDevice.SOURCE_MOUSE_RELATIVE to "MOUSE_REL", InputDevice.SOURCE_JOYSTICK to "JOYSTICK",
            InputDevice.SOURCE_GAMEPAD to "GAMEPAD", InputDevice.SOURCE_TOUCH_NAVIGATION to "TOUCH_NAV",
            InputDevice.SOURCE_TRACKBALL to "TRACKBALL"
        ).filter { (bits, _) -> source and bits == bits }.map { it.second }
        return if (names.isEmpty()) "0x%x".format(source) else names.joinToString("|")
    }

    private companion object {
        const val TAG = "AAB_INPUT"
        const val MAX_LINES = 8
        const val HIDE_AFTER_MS = 15000L
    }
}
