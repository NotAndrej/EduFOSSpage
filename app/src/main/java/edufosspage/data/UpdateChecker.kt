package org.edufosspage.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.edufosspage.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl:   String,
    val checksumUrl:   String?,
)

object UpdateChecker {

    private const val TAG           = "UpdateChecker"
    private const val API_URL       = "https://api.github.com/repos/NotAndrej/EduFOSSpage/releases/latest"
    private const val APK_NAME      = "efp_latest.apk"
    private const val CHECKSUM_NAME = "efp_latest.apk.sha256"

    fun isNewer(current: String, latest: String): Boolean {
        fun parts(v: String) = v.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
        val c = parts(current); val l = parts(latest)
        val len = maxOf(c.size, l.size)
        for (i in 0 until len) {
            val cv = c.getOrElse(i) { 0 }; val lv = l.getOrElse(i) { 0 }
            if (lv > cv) return true; if (lv < cv) return false
        }
        return false
    }

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept",     "application/vnd.github+json")
                setRequestProperty("User-Agent", "EduFOSSpage/${BuildConfig.VERSION_NAME}")
                connectTimeout = 10_000; readTimeout = 10_000
            }
            if (conn.responseCode != 200) {
                Log.w(TAG, "GitHub API returned ${conn.responseCode}"); return@runCatching null
            }

            val json   = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag    = json.optString("tag_name").ifBlank { return@runCatching null }
            val assets = json.optJSONArray("assets") ?: return@runCatching null

            var downloadUrl: String? = null
            var checksumUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                when (asset.optString("name")) {
                    APK_NAME      -> downloadUrl = asset.optString("browser_download_url").ifBlank { null }
                    CHECKSUM_NAME -> checksumUrl = asset.optString("browser_download_url").ifBlank { null }
                }
            }

            if (downloadUrl == null) {
                Log.w(TAG, "No $APK_NAME asset in release $tag"); return@runCatching null
            }
            if (checksumUrl == null) Log.w(TAG, "No $CHECKSUM_NAME in release $tag - integrity check skipped")

            Log.d(TAG, "Latest: $tag  Current: ${BuildConfig.VERSION_NAME}")
            if (!isNewer(BuildConfig.VERSION_NAME, tag)) return@runCatching null

            UpdateInfo(latestVersion = tag.trimStart('v'), downloadUrl = downloadUrl, checksumUrl = checksumUrl)
        }.getOrElse { Log.w(TAG, "Update check failed: ${it.message}"); null }
    }

    // Downloads the APK and verifies the SHA-256 checksum if available. Returns null on failure or mismatch.
    suspend fun download(
        ctx:         Context,
        url:         String,
        checksumUrl: String?,
        onProgress:  (downloaded: Long, total: Long) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val outDir  = File(ctx.cacheDir, "updates").also { it.mkdirs() }
            val outFile = File(outDir, APK_NAME)

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "EduFOSSpage/${BuildConfig.VERSION_NAME}")
                connectTimeout = 15_000; readTimeout = 60_000
            }
            val total = conn.contentLength.toLong()
            var downloaded = 0L

            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(8 * 1024); var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n); downloaded += n; onProgress(downloaded, total)
                    }
                }
            }
            onProgress(total, total)

                    if (checksumUrl != null) {
                if (!verifyChecksum(outFile, checksumUrl)) {
                    outFile.delete()
                    return@runCatching null
                }
            }

            Log.i(TAG, "APK ready: ${outFile.absolutePath}")
            outFile
        }.getOrElse { Log.e(TAG, "Download failed: ${it.message}"); null }
    }

    // Downloads the .sha256 file (supports both "hexdigest" and "hexdigest  filename" formats)
    // and verifies it against [file].
    private fun verifyChecksum(file: File, checksumUrl: String): Boolean = runCatching {
        val conn = (URL(checksumUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "EduFOSSpage/${BuildConfig.VERSION_NAME}")
            connectTimeout = 10_000; readTimeout = 10_000
        }
        val raw         = conn.inputStream.bufferedReader().readText().trim()
        val expectedHex = raw.split("\\s+".toRegex()).first().lowercase()

        if (expectedHex.length != 64) {
            Log.w(TAG, "Unexpected checksum format: $raw"); return@runCatching false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8 * 1024); var n: Int
            while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        val actualHex = digest.digest().joinToString("") { "%02x".format(it) }

        (actualHex == expectedHex).also { ok ->
            if (ok) Log.i(TAG, "SHA-256 verified ✓")
            else    Log.e(TAG, "SHA-256 mismatch: expected=$expectedHex actual=$actualHex")
        }
    }.getOrElse { Log.e(TAG, "Checksum verification error: ${it.message}"); false }

    fun install(ctx: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apkFile)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun cachedApk(ctx: Context): File? =
        File(File(ctx.cacheDir, "updates"), APK_NAME).takeIf { it.exists() }
}
