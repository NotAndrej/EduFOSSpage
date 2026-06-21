package org.edufosspage.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class EduMessage(
    val id:        String,
    val sender:    String,
    val preview:   String,
    val timestamp: String,
    val date:      String,
    val time:      String,
    val type:      String,
)

object MessagesApi {

    private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun fetchMessages(daysBack: Int = 30): List<EduMessage>? {
        val to   = LocalDate.now()
        val from = to.minusDays(daysBack.toLong())
        return fetchMessages(from, to)
    }

    /**
     * Fetch messages between [from] (inclusive) and [to] (inclusive).
     * Returns null on network/parse error, empty list when the range has no messages.
     */
    suspend fun fetchMessages(from: LocalDate, to: LocalDate): List<EduMessage>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dateFrom = from.format(DATE_FMT)
            val dateTo   = to.format(DATE_FMT)

            val params = "module=todo&filterTab=&akcia=getData&filterTab=messages"
            val body   = "datefrom=${URLEncoder.encode(dateFrom, "UTF-8")}" +
                         "&dateto=${URLEncoder.encode(dateTo, "UTF-8")}"
            val url    = "https://${EduSession.subdomain}.edupage.org/timeline/?$params"

            Log.d("EduMessages", "POST $url  [$dateFrom → $dateTo]")

            val responseText = EduHttp.post(url, body, "EduMessages")
                ?: return@withContext null

            val json = JSONObject(responseText)

            if (!json.has("timelineItems")) {
                Log.e("EduMessages", "no timelineItems, keys=${json.keys().asSequence().toList()}")
                return@withContext emptyList()
            }

            val items  = json.getJSONArray("timelineItems")
            val result = mutableListOf<EduMessage>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)

                if (item.optString("removed") == "1") continue

                val sender = item.optString("vlastnik_meno").ifBlank {
                    item.optString("user_meno").ifBlank { "Unknown" }
                }

                var text = item.optString("text")
                if (text.isBlank() || text == "null") {
                    text = runCatching {
                        val dataStr = item.optString("data")
                        if (dataStr.startsWith("{")) {
                            val d = JSONObject(dataStr)
                            d.optString("messageContent").ifBlank {
                                d.optString("nazov").ifBlank { d.optString("text") }
                            }
                        } else dataStr
                    }.getOrElse { "" }
                }

                val fullText = text
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&nbsp;", " ")
                    .replace("&amp;",  "&")
                    .replace("&lt;",   "<")
                    .replace("&gt;",   ">")
                    .replace("&quot;", "\"")
                    .trim()

                val timestamp = item.optString("timestamp").ifBlank { item.optString("cas_pridania") }
                val date      = timestamp.take(10)
                val time      = if (timestamp.length >= 16) timestamp.substring(11, 16) else ""
                val id        = item.optString("timelineid").ifBlank { "$i" }
                val type      = item.optString("typ")

                result.add(EduMessage(
                    id        = id,
                    sender    = sender,
                    preview   = fullText.ifBlank { type },
                    timestamp = timestamp,
                    date      = date,
                    time      = time,
                    type      = type,
                ))
            }

            Log.d("EduMessages", "parsed ${result.size} messages for [$dateFrom → $dateTo]")
            result.sortedByDescending { it.timestamp }

        }.getOrElse { e ->
            Log.e("EduMessages", "fetchMessages: ${e.message}", e)
            null
        }
    }
}
