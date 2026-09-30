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

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.ClientCertRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewFeature
import com.kododake.aabrowser.R
import com.kododake.aabrowser.data.BrowserPreferences

class BrowserWebViewClient(
    private val callbacks: BrowserCallbacks
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (CleartextNavigationHandler.handleCleartextIfNeeded(view, uri, callbacks, onPageStart = false)) {
            return true
        }
        return handleUri(uri)
    }

    private fun handleUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme in setOf("http", "https", "about", "file", "data", "javascript")) {
            return false
        }
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val stringUrl = url ?: return
        callbacks.onPageStarted(stringUrl)
        if (stringUrl.contains("youtube.com")) {
            view.evaluateJavascript(WidescreenScripts.YOUTUBE_PREEMPTIVE_JS, null)
        }
        val uri = Uri.parse(stringUrl)
        val scheme = uri.scheme?.lowercase()

        if (scheme == "http") {
            val allowedOnce = view.getTag(R.id.webview_allow_once_uri_tag) as? String
            if (allowedOnce == stringUrl) {
                view.setTag(R.id.webview_allow_once_uri_tag, null)
            } else if (CleartextNavigationHandler.handleCleartextIfNeeded(view, uri, callbacks, onPageStart = true)) {
                return
            }
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        view.evaluateJavascript(SpeechRecognitionBridge.POLYFILL_JS, null)
        view.evaluateJavascript(WidescreenScripts.RESTORE_UI_JS, null)
        if (url?.contains("youtube.com") == true) {
            view.evaluateJavascript(WidescreenScripts.YOUTUBE_LAYOUT_FIX_JS, null)
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            if (BrowserPreferences.isDrmL3EnforcerEnabled(view.context)) {
                view.evaluateJavascript(WebScripts.DRM_L3_ENFORCER_JS, null)
            }
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.NAVIGATION_LISTENER)) {
            url?.let(callbacks.onUrlChange)
        }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
            val code = error.errorCode
            val shouldShowErrorPage = when (code) {
                ERROR_HOST_LOOKUP,
                ERROR_CONNECT,
                ERROR_TIMEOUT,
                ERROR_UNKNOWN,
                ERROR_PROXY_AUTHENTICATION -> true
                else -> false
            }

            if (shouldShowErrorPage) {
                val failed = request.url?.toString().orEmpty()
                val message = error.description?.toString().orEmpty()
                val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                try {
                    view.loadUrl(assetUrl)
                } catch (_: Exception) {
                    callbacks.onError(code, error.description?.toString())
                }
                return
            }
        }
        callbacks.onError(error.errorCode, error.description?.toString())
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) {
            val code = errorResponse.statusCode
            if (code in 400..599 && code != 429) {
                val failed = request.url?.toString().orEmpty()
                val message = errorResponse.reasonPhrase.orEmpty()
                val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                try {
                    view.loadUrl(assetUrl)
                } catch (_: Exception) {
                    callbacks.onError(code, message)
                }
                return
            }
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val activity = view.context as? Activity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            SslErrorHandlerHelper.handleSslError(activity, handler, error)
        } else {
            handler.cancel()
        }
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        val activity = view.context as? Activity
        if (activity != null) {
            ClientCertHandler.handleClientCertRequest(activity, request)
        } else {
            request.cancel()
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        val parent = view.parent as? ViewGroup
        parent?.removeView(view)
        view.destroy()
        callbacks.onRenderProcessGone(detail.didCrash())
        return true
    }
}
