package org.edufosspage.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Permanent encrypted cache for full message history.
 * Unlike AppCache, these files never expire - they accumulate all messages
 * fetched since the user first opened the app.
 */
object MessageHistoryCache {

    private const val TAG       = "MsgHistoryCache"
    private const val META_FILE = "msg_history_meta.json"
    private val MONTH_FMT       = DateTimeFormatter.ofPattern("yyyy-MM")
    private val DATE_FMT        = DateTimeFormatter.ISO_LOCAL_DATE

    data class Meta(
        val oldestFetchedDate: LocalDate,
        /** True once the API returned 0 results - no more history to fetch. */
        val reachedBeginning:  Boolean,
    )

    private fun metaFile(ctx: Context) = File(ctx.filesDir, META_FILE)

    fun loadMeta(ctx: Context): Meta {
        val f = metaFile(ctx)
        if (!f.exists()) return Meta(LocalDate.now().minusDays(30), false)
        return runCatching {
            val j = JSONObject(EncryptedStore.read(ctx, f) ?: return@runCatching null)
            Meta(
                oldestFetchedDate = LocalDate.parse(j.optString("oldestFetchedDate"), DATE_FMT),
                reachedBeginning  = j.optBoolean("reachedBeginning", false),
            )
        }.getOrElse {
            Log.e(TAG, "loadMeta: ${it.message}")
            Meta(LocalDate.now().minusDays(30), false)
        } ?: Meta(LocalDate.now().minusDays(30), false)
    }

    fun saveMeta(ctx: Context, meta: Meta) {
        runCatching {
            EncryptedStore.write(
                ctx, metaFile(ctx),
                JSONObject()
                    .put("oldestFetchedDate", meta.oldestFetchedDate.format(DATE_FMT))
                    .put("reachedBeginning",  meta.reachedBeginning)
                    .toString()
            )
        }.onFailure { Log.e(TAG, "saveMeta: ${it.message}") }
    }

    private fun monthFile(ctx: Context, yearMonth: String) =
        File(ctx.filesDir, "msg_history_$yearMonth.json")

    private fun allMonthFiles(ctx: Context): List<File> =
        ctx.filesDir.listFiles()
            ?.filter { it.name.startsWith("msg_history_") && it.name != META_FILE }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    /** Load all cached messages from disk, sorted newest first. */
    suspend fun loadAll(ctx: Context): List<EduMessage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<EduMessage>()
        for (file in allMonthFiles(ctx)) {
            runCatching {
                val text = EncryptedStore.read(ctx, file) ?: return@runCatching
                val arr  = JSONArray(text)
                for (i in 0 until arr.length()) result.add(fromJson(arr.getJSONObject(i)))
            }.onFailure { Log.e(TAG, "loadAll ${file.name}: ${it.message}") }
        }
        result.sortedByDescending { it.timestamp }
    }

    suspend fun saveBatch(
        ctx:           Context,
        messages:      List<EduMessage>,
        batchFromDate: LocalDate,
        isLastChunk:   Boolean = false,
    ) = withContext(Dispatchers.IO) {
        if (!AppCache.isCacheEnabled(ctx)) return@withContext

        // Group by month (date is "YYYY-MM-DD", take first 7 chars)
        val byMonth = messages.groupBy { msg ->
            msg.date.take(7).ifBlank { batchFromDate.format(MONTH_FMT) }
        }

        byMonth.forEach { (month, msgs) ->
            val file = monthFile(ctx, month)
            val existing = runCatching {
                val text = EncryptedStore.read(ctx, file) ?: return@runCatching emptyList()
                val arr  = JSONArray(text)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }.getOrElse { emptyList() }

            val merged = (existing + msgs)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }

            runCatching {
                EncryptedStore.write(ctx, file, JSONArray(merged.map { toJson(it) }).toString())
                Log.v(TAG, "saved ${merged.size} messages to msg_history_$month.json")
            }.onFailure { Log.e(TAG, "saveBatch $month: ${it.message}") }
        }

        val meta      = loadMeta(ctx)
        val newOldest = if (batchFromDate < meta.oldestFetchedDate) batchFromDate else meta.oldestFetchedDate
        saveMeta(ctx, meta.copy(oldestFetchedDate = newOldest, reachedBeginning = isLastChunk))
    }

    fun clearAll(ctx: Context) {
        ctx.filesDir.listFiles()
            ?.filter { it.name.startsWith("msg_history_") }
            ?.forEach { it.delete() }
        Log.i(TAG, "history cache cleared")
    }

    private fun toJson(m: EduMessage) = JSONObject().apply {
        put("id",        m.id)
        put("sender",    m.sender)
        put("preview",   m.preview)
        put("timestamp", m.timestamp)
        put("date",      m.date)
        put("time",      m.time)
        put("type",      m.type)
    }

    private fun fromJson(j: JSONObject) = EduMessage(
        id        = j.optString("id"),
        sender    = j.optString("sender"),
        preview   = j.optString("preview"),
        timestamp = j.optString("timestamp"),
        date      = j.optString("date"),
        time      = j.optString("time"),
        type      = j.optString("type"),
    )
}
