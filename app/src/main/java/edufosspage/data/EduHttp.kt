package org.edufosspage.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shared HTTP client for all EduPage API calls.
 * Checks connectivity before every request, returns null immediately if
 * the device is offline so ViewModels treat it the same as a network error.
 */
internal object EduHttp {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS    = 15_000

    // Application context set once in LoginViewModel.initSession()
    var appContext: Context? = null

    fun isOnline(): Boolean {
        val ctx = appContext ?: return false
        val cm  = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(net) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun get(urlStr: String, tag: String = "EduHttp"): String? {
        if (!isOnline()) { Log.d(tag, "offline skipping GET $urlStr"); return null }
        return runCatching {
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod           = "GET"
                instanceFollowRedirects = true
                connectTimeout          = CONNECT_TIMEOUT_MS
                readTimeout             = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Cookie",     EduSession.cookieHeader())
            }.inputStream.bufferedReader().readText()
        }.getOrElse { Log.e(tag, "GET $urlStr: ${it.message}"); null }
    }

    fun post(urlStr: String, body: String, tag: String = "EduHttp"): String? {
        if (!isOnline()) { Log.d(tag, "offline skipping POST $urlStr"); return null }
        return runCatching {
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod           = "POST"
                doOutput                = true
                instanceFollowRedirects = true
                connectTimeout          = CONNECT_TIMEOUT_MS
                readTimeout             = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent",   "Mozilla/5.0")
                setRequestProperty("Cookie",       EduSession.cookieHeader())
                outputStream.use { it.write(body.toByteArray()) }
            }.inputStream.bufferedReader().readText()
        }.getOrElse { Log.e(tag, "POST $urlStr: ${it.message}"); null }
    }
}
