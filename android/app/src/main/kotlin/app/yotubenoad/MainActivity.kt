package app.yotubenoad

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class MainActivity : Activity() {

    private companion object {
        const val HOME = "https://m.youtube.com/"
    }

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var progress: View
    private lateinit var pill: TextView
    private lateinit var shield: ShieldButton
    private lateinit var splash: FrameLayout

    private val prefs by lazy { getSharedPreferences("yna", MODE_PRIVATE) }
    private val blocked = AtomicInteger(0)
    private var enabled = true
    private var scriptHandler: ScriptHandler? = null
    private var scriptText = ""

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    private var splashShown = SystemClock.elapsedRealtime()
    private var splashGone = false

    private val dim = Runnable { shield.animate().alpha(0.45f).setDuration(600).start() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enabled = prefs.getBoolean("enabled", true)

        root = FrameLayout(this).apply { setBackgroundColor(Palette.BG) }

        web = WebView(this).apply {
            setBackgroundColor(Palette.BG)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            addJavascriptInterface(Bridge(), "YNA")
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }

        progress = View(this).apply {
            setBackgroundColor(Palette.ON)
            pivotX = 0f
            scaleX = 0f
        }

        pill = TextView(this).apply {
            textSize = 13f
            setTextColor(Palette.TEXT)
            setPadding(dp(16), dp(9), dp(16), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Palette.SURFACE)
                setStroke(dp(1), Palette.SURFACE_STROKE)
            }
            alpha = 0f
            visibility = View.GONE
        }

        shield = ShieldButton(this).apply {
            setInstant(enabled)
            scaleX = 0f
            scaleY = 0f
            setOnClickListener { toggle() }
        }

        splash = FrameLayout(this).apply {
            setBackgroundColor(Palette.BG)
            val title = TextView(context).apply {
                text = "YotubeNoAd"
                textSize = 20f
                letterSpacing = 0.06f
                setTextColor(Palette.TEXT)
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                alpha = 0f
                animate().alpha(1f).setDuration(500).start()
            }
            addView(title, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }

        root.addView(web, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(progress, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(2), Gravity.TOP))
        root.addView(pill, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        root.addView(shield, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM or Gravity.END))
        root.addView(splash, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = max(bars.bottom, ime.bottom)

            (web.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = bars.top; bottomMargin = bottom; leftMargin = bars.left; rightMargin = bars.right
            }
            (progress.layoutParams as FrameLayout.LayoutParams).topMargin = bars.top
            (shield.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = bottom + dp(84); rightMargin = bars.right + dp(14)
            }
            (pill.layoutParams as FrameLayout.LayoutParams).bottomMargin = bottom + dp(90)
            root.requestLayout()
            insets
        }

        web.webViewClient = client
        web.webChromeClient = chrome

        installScript()
        web.loadUrl(resolveUrl(intent))

        // aciliste kalkan yumusakca belirir, bir sure sonra silikles
        splash.postDelayed({ hideSplash() }, 6000)
    }

    // ---- Reklam engelleme betigi ----

    private fun installScript() {
        scriptHandler?.remove()
        scriptHandler = null
        scriptText = AdBlock.script(this, enabled)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(web, scriptText, setOf("https://*.youtube.com"))
        }
    }

    private fun toggle() {
        enabled = !enabled
        prefs.edit().putBoolean("enabled", enabled).apply()
        shield.setOn(enabled)
        installScript()
        showPill(statusText())
        wake()
        web.reload()
    }

    private fun statusText(): String {
        if (!enabled) return "Reklam engelleme kapalı"
        val n = blocked.get()
        return if (n > 0) "Reklam engelleme açık  ·  $n engellendi" else "Reklam engelleme açık"
    }

    inner class Bridge {
        @JavascriptInterface
        fun blocked(n: Int) {
            blocked.addAndGet(n)
        }
    }

    // ---- Animasyonlu yardimcilar ----

    private fun showPill(text: String) {
        pill.animate().cancel()
        pill.text = text
        pill.visibility = View.VISIBLE
        pill.alpha = 0f
        pill.translationY = dp(8).toFloat()
        pill.animate().alpha(1f).translationY(0f).setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                pill.animate().alpha(0f).translationY(-dp(4).toFloat())
                    .setStartDelay(1600).setDuration(260)
                    .withEndAction { pill.visibility = View.GONE }
                    .start()
            }.start()
    }

    private fun wake() {
        shield.animate().alpha(1f).setDuration(180).start()
        shield.removeCallbacks(dim)
        shield.postDelayed(dim, 2500)
    }

    private fun hideSplash() {
        if (splashGone) return
        splashGone = true
        val wait = max(0L, 900L - (SystemClock.elapsedRealtime() - splashShown))
        splash.animate().alpha(0f).setStartDelay(wait).setDuration(380)
            .withEndAction { splash.visibility = View.GONE }
            .start()
        shield.animate().scaleX(1f).scaleY(1f).setStartDelay(wait + 250).setDuration(420)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                showPill(statusText())
                wake()
            }.start()
    }

    private fun updateProgress(p: Int) {
        progress.animate().cancel()
        if (p < 100) {
            progress.alpha = 1f
            progress.animate().scaleX(max(p, 8) / 100f).setDuration(200).start()
        } else {
            progress.animate().scaleX(1f).setDuration(150).withEndAction {
                progress.animate().alpha(0f).setDuration(250)
                    .withEndAction { progress.scaleX = 0f }.start()
            }.start()
        }
    }

    // ---- WebView istemcileri ----

    private val client = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (enabled && AdBlock.shouldBlock(request.url)) {
                blocked.incrementAndGet()
                return AdBlock.empty()
            }
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val u = request.url
            val scheme = u.scheme ?: return true
            if (scheme != "http" && scheme != "https") return true
            val host = u.host ?: return false
            if (host == "youtu.be") {
                view.loadUrl(resolveUrl(Intent(Intent.ACTION_VIEW, u)))
                return true
            }
            if (isAllowedHost(host)) return false
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, u)) }
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (scriptHandler == null) view.evaluateJavascript(scriptText, null)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            hideSplash()
        }
    }

    private val chrome = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            updateProgress(newProgress)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view == null) return
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            customView = view
            customCallback = callback
            view.setBackgroundColor(Color.BLACK)
            root.addView(view, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            web.visibility = View.GONE
            shield.visibility = View.GONE
            applyImmersive(true)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        override fun onHideCustomView() {
            val v = customView ?: return
            root.removeView(v)
            customView = null
            customCallback?.onCustomViewHidden()
            customCallback = null
            web.visibility = View.VISIBLE
            shield.visibility = View.VISIBLE
            applyImmersive(false)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun applyImmersive(on: Boolean) {
        val c = WindowCompat.getInsetsController(window, root)
        c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (on) c.hide(WindowInsetsCompat.Type.systemBars()) else c.show(WindowInsetsCompat.Type.systemBars())
    }

    // ---- Adres yonetimi ----

    private fun isAllowedHost(host: String): Boolean =
        host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "google.com" || host.endsWith(".google.com")

    private fun resolveUrl(i: Intent?): String {
        val d: Uri = i?.data ?: return HOME
        val host = d.host ?: return HOME
        if (host == "youtu.be") {
            val id = d.lastPathSegment ?: return HOME
            val extra = d.query?.let { "&$it" } ?: ""
            return "https://m.youtube.com/watch?v=$id$extra"
        }
        return if (isAllowedHost(host)) d.toString().replace("://www.youtube.com", "://m.youtube.com") else HOME
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data != null) web.loadUrl(resolveUrl(intent))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> chrome.onHideCustomView()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onPause() {
        web.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        root.removeAllViews()
        web.destroy()
        super.onDestroy()
    }
}
