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

import android.webkit.WebView
import androidx.webkit.NavigationParameters
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

object WebViewNavigationHelper {

    @WebViewCompat.ExperimentalNavigate
    fun navigate(webView: WebView, url: String, replaceCurrentEntry: Boolean = false) {
        if (url.isBlank()) return
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1)) {
            runCatching {
                val params = NavigationParameters.Builder()
                    .setShouldReplaceCurrentEntry(replaceCurrentEntry)
                    .build()
                WebViewCompat.navigate(webView, url, params)
            }.onFailure {
                webView.loadUrl(url)
            }
        } else {
            webView.loadUrl(url)
        }
    }
}
