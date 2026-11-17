package app.yotubenoad

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** Ag seviyesinde reklam istegi engelleme + sayfaya eklenen betik. */
object AdBlock {
    private val adHosts = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
    )
    private val adPaths = listOf("/pagead/", "/api/stats/ads", "/get_midroll_", "/ptracking")

    fun shouldBlock(url: Uri): Boolean {
        val host = url.host ?: return false
        if (adHosts.any { host == it || host.endsWith(".$it") }) return true
        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            val path = url.encodedPath ?: return false
            return adPaths.any { path.startsWith(it) }
        }
        return false
    }

    fun empty(): WebResourceResponse = WebResourceResponse(
        "text/plain", "utf-8", 204, "No Content",
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(ByteArray(0)),
    )

    fun script(context: Context, enabled: Boolean): String {
        val js = context.assets.open("adblock.js").bufferedReader().use { it.readText() }
        return "window.__YNA_OFF=${!enabled};\n$js"
    }
}
