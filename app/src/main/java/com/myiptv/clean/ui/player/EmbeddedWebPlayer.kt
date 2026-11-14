package com.myiptv.clean.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.util.Collections

/** A sandboxed page player. The playback bot uses same-origin DOM; no script has a native JS bridge. */
internal class EmbeddedWebPlayer(
    context: Context,
    private val sourceUrl: String,
    script: String,
    onState: (EmbedBotState) -> Unit,
    private val onLoading: (Boolean) -> Unit,
    private val onError: (String?) -> Unit,
    private val onNotice: (String) -> Unit,
    private val onBlockedRedirect: (String?) -> Unit,
    private val onFullscreen: (Boolean) -> Unit
) {
    val container = FrameLayout(context)
    val webView = WebView(context)
    private var lastState = EmbedBotState()
    val bot = EmbedPlaybackBot(webView, script) { state ->
        lastState = state
        onState(state)
    }
    private val policy = EmbedNavigationPolicy(sourceUrl)
    private val approvedHosts = Collections.synchronizedSet(mutableSetOf<String>())
    private var pendingRedirect: String? = null
    private var pageCommitted = false
    private var disposed = false
    private var blankWhileBackground = false
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val adGuardScript = runCatching {
        context.assets.open("samplay_frame_adguard.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrDefault("")
    private var adGuardAtDocumentStart = false

    init {
        container.setBackgroundColor(android.graphics.Color.BLACK)
        container.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            // With multiple windows enabled, onCreateWindow can reject popups instead of loading
            // them in the current WebView on some Android versions.
            setSupportMultipleWindows(true)
            setGeolocationEnabled(false)
        }
        // Unlike evaluateJavascript on the top page, document-start registration also runs in
        // cross-origin iframe documents. Older WebViews use the same-origin fallback below.
        if (adGuardScript.isNotBlank()) {
            adGuardAtDocumentStart = try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(webView, adGuardScript, setOf("*"))
                    true
                } else false
            } catch (_: Exception) {
                false
            } catch (_: LinkageError) {
                false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean = false

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.deny()
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null || customView != null || disposed) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                container.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                view.requestFocus()
                onFullscreen(true)
            }

            override fun onHideCustomView() = hideFullscreen()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val allowed = isAllowed(url, request.isForMainFrame)
                if (!allowed && request.isForMainFrame && !disposed) reportBlockedRedirect(url, forceError = request.hasGesture())
                return !allowed
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // This legacy callback cannot distinguish child frames from the main frame.
                // Keep non-ad child players usable; onPageStarted checks main-frame redirects.
                return !isAllowed(url, isMainFrame = false)
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                return if (policy.isAdRequest(url) || (request.isForMainFrame && !isAllowed(url, true))) blockedResponse() else null
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
                if (policy.isAdRequest(url)) blockedResponse() else null

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (disposed || blankWhileBackground || url == "about:blank") return
                if (!isAllowed(url, isMainFrame = true)) {
                    pageCommitted = false
                    view.stopLoading()
                    reportBlockedRedirect(url, forceError = true)
                    return
                }
                pageCommitted = false
                pendingRedirect = null
                onBlockedRedirect(null)
                bot.onPageStarted()
                onError(null)
                onLoading(true)
            }

            override fun onPageCommitVisible(view: WebView, url: String) {
                // Some player pages keep network requests open for a long time; start the bot
                // as soon as the committed page is visible instead of waiting for every resource.
                if (!disposed && !blankWhileBackground && isAllowed(url, isMainFrame = true)) {
                    pageCommitted = true
                    onLoading(false)
                    installFallbackAdGuard(view)
                    bot.onPageFinished()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!disposed && !blankWhileBackground && isAllowed(url, isMainFrame = true)) {
                    pageCommitted = true
                    onLoading(false)
                    installFallbackAdGuard(view)
                    bot.onPageFinished()
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!disposed && request.isForMainFrame) {
                    onLoading(false)
                    onError("No se pudo cargar el reproductor web. Comprueba la conexión e inténtalo de nuevo.")
                }
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                if (Build.VERSION.SDK_INT < 23 && !disposed && failingUrl == view.url) {
                    onLoading(false)
                    onError("No se pudo cargar el reproductor web. Comprueba la conexión e inténtalo de nuevo.")
                }
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (!disposed && request.isForMainFrame && errorResponse.statusCode >= 400) {
                    onLoading(false)
                    onError("El proveedor no pudo abrir este enlace (${errorResponse.statusCode}).")
                }
            }
        }
        webView.loadUrl(sourceUrl)
    }

    private fun installFallbackAdGuard(view: WebView) {
        if (!adGuardAtDocumentStart && adGuardScript.isNotBlank()) {
            runCatching { view.evaluateJavascript(adGuardScript, null) }
        }
    }

    fun onForeground() {
        if (disposed) return
        webView.onResume()
        bot.foreground()
        if (blankWhileBackground) {
            blankWhileBackground = false
            webView.loadUrl(sourceUrl)
        }
    }

    fun onBackground() {
        if (disposed) return
        bot.background()
        if (!lastState.controllable) {
            // An inaccessible cross-origin frame cannot be paused by the bot. Unload it so it
            // cannot keep playing sound in the background; reload it when the app returns.
            blankWhileBackground = true
            bot.onPageStarted()
            hideFullscreen()
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
        webView.onPause()
    }

    fun reload() {
        if (!disposed) webView.loadUrl(sourceUrl)
    }

    /** Only an explicit user action can allow an unknown destination, for this player instance. */
    fun approvePendingRedirect() {
        if (disposed) return
        val url = pendingRedirect ?: return
        val host = policy.approvableHost(url) ?: return
        approvedHosts.add(host)
        pendingRedirect = null
        onBlockedRedirect(null)
        onError(null)
        webView.loadUrl(url)
    }

    fun dismissPendingRedirect() {
        pendingRedirect = null
        if (!disposed) {
            onBlockedRedirect(null)
            if (pageCommitted) onError(null)
        }
    }

    fun hideFullscreen() {
        val view = customView ?: return
        container.removeView(view)
        customView = null
        val callback = customViewCallback
        customViewCallback = null
        webView.visibility = View.VISIBLE
        callback?.onCustomViewHidden()
        if (!disposed) onFullscreen(false)
    }

    fun release() {
        if (disposed) return
        bot.dispose()
        hideFullscreen()
        disposed = true
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.removeAllViews()
        container.removeAllViews()
        webView.destroy()
    }

    private fun isAllowed(url: String, isMainFrame: Boolean): Boolean =
        policy.allowsNavigation(url, isMainFrame, approvedHosts)

    private fun reportBlockedRedirect(url: String, forceError: Boolean) {
        if (pageCommitted && policy.isAdRequest(url)) {
            onNotice("Se bloqueó una redirección publicitaria.")
            return
        }
        if (forceError || !pageCommitted) {
            val host = policy.approvableHost(url)
            pendingRedirect = url.takeIf { host != null }
            onBlockedRedirect(host)
            onLoading(false)
            onError("Se bloqueó una redirección fuera del reproductor.")
        } else {
            onNotice("Se bloqueó una redirección fuera del reproductor.")
        }
    }

    private fun blockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "UTF-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0))
    )
}
