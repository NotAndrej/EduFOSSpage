package org.edufosspage.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class LoginResult {
    data class Success(val schoolUrl: String, val cookies: List<String>) : LoginResult()
    data class Failure(val error: String) : LoginResult()
}

object EdupageLogin {
    private val cookieStore = mutableListOf<String>()

    suspend fun login(username: String, password: String): LoginResult =
    withContext(Dispatchers.IO) {
        cookieStore.clear()
        runCatching {
            val loginPageHtml = get("https://login1.edupage.org/login/?cmd=MainLogin")
                ?: return@runCatching LoginResult.Failure("Couldn't reach login page")

            val csrfToken = loginPageHtml
                .split("\"csrftoken\":\"").getOrNull(1)
                ?.split("\"")?.getOrNull(0)
                ?: return@runCatching LoginResult.Failure("Couldn't find CSRF token")

            val postData = "csrfauth=${enc(csrfToken)}&username=${enc(username)}&password=${enc(password)}"
            val (responseBody, finalUrl) = post(
                "https://login1.edupage.org/login/edubarLogin.php", postData
            ) ?: return@runCatching LoginResult.Failure("Login request failed")

            Log.d("EduLogin", "Final URL: $finalUrl")

            when {
                "bad=1"       in finalUrl -> return@runCatching LoginResult.Failure("Wrong username or password")
                "cap=1"       in finalUrl ||
                "lerr=b43b43" in finalUrl -> return@runCatching LoginResult.Failure("Captcha required try via browser first")
                "twofactor"   in finalUrl -> return@runCatching LoginResult.Failure("This account uses 2FA which is not supported yet")
            }

            val subdomain = responseBody
                .split("-->").getOrNull(0)
                ?.trim()?.split(" ")?.lastOrNull()
                ?.takeIf { it.isNotBlank() && !it.contains("<") }
                ?: URL(finalUrl).host.split(".")[0]

            if ("eqa=" in finalUrl) {
                runCatching {
                    val eqaEncoded = finalUrl.substringAfter("eqa=")
                    val eqaUrlDec  = java.net.URLDecoder.decode(eqaEncoded, "UTF-8")
                    val eqaDecoded = android.util.Base64
                        .decode(eqaUrlDec, android.util.Base64.DEFAULT)
                        .toString(Charsets.UTF_8)
                    val gu = eqaDecoded.split("gu=").getOrNull(1)
                        ?.split("&")?.getOrNull(0) ?: "/user/"
                    get("https://$subdomain.edupage.org$gu")
                }.onFailure { Log.w("EduLogin", "eqa confirm failed: ${it.message}") }
            }

            LoginResult.Success("https://$subdomain.edupage.org/user", cookieStore.toList())

        }.getOrElse { LoginResult.Failure("${it.javaClass.simpleName}: ${it.message}") }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout    = 15_000
            setRequestProperty("User-Agent", "Mozilla/5.0")
            if (cookieStore.isNotEmpty())
                setRequestProperty("Cookie", cookieStore.joinToString("; ") { it.split(";")[0] })
        }
        conn.headerFields["Set-Cookie"]?.let { cookieStore.addAll(it) }
        return runCatching { conn.inputStream.bufferedReader().readText() }.getOrNull()
    }

    private fun post(urlStr: String, body: String): Pair<String, String>? {
        var currentUrl = urlStr
        repeat(10) { hop ->
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                if (cookieStore.isNotEmpty()) {
                    setRequestProperty("Cookie", cookieStore.joinToString("; ") { it.split(";")[0] })
                }
            }

            if (hop == 0 && body.isNotEmpty()) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            } else {
                conn.requestMethod = "GET"
            }

            conn.headerFields["Set-Cookie"]?.let { cookieStore.addAll(it) }
            val code     = conn.responseCode
            val location = conn.getHeaderField("Location")
            Log.d("EduLogin", "[$hop] $currentUrl → $code ${location ?: ""}")

            if (code in 300..399 && location != null) {
                var nextUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                    location
                } else {
                    URL(URL(currentUrl), location).toString()
                }

                // Upgrade http -> https
                if (nextUrl.startsWith("http://")) {
                    nextUrl = nextUrl.replaceFirst("http://", "https://")
                }

                currentUrl = nextUrl
            } else {
                val lastBody = runCatching {
                    conn.inputStream.bufferedReader().use { it.readText() }
                }.getOrElse {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                return Pair(lastBody, currentUrl)
            }
        }
        return null
    }
}
