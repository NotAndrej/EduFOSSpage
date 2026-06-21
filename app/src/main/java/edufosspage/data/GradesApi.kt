package org.edufosspage.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

data class EduGrade(
    val eventId:      String,
    val title:        String,
    val gradeValue:   String,       // raw display: "2", "A", "25b", etc.
    val gradeNumeric: Float?,       // null if verbal / non-numeric
    val date:         String,       // "yyyy-MM-dd"
    val subjectId:    String,
    val subjectName:  String,
    val subjectShort: String,
    val teacherName:  String,
    val maxPoints:    Float?,       // null for normal 1-5 grades
    val importance:   Float?,       // 1.0-5.0 weight (p_vaha/20), null for points grades
    val percent:      Float?,       // 0-100, null for verbal
    val classAvg:     Float?,       // class average, null if not provided
    val isVerbal:     Boolean,
    val gradeType:    Int,          // 1=normal 1-5, 2=points, 3=percent
)

data class SubjectGrades(
    val subjectId:    String,
    val subjectName:  String,
    val subjectShort: String,
    val grades:       List<EduGrade>,
) {
    /** True when this subject uses percentage-based grading */
    val isPercentSubject: Boolean get() = grades.any { it.gradeType == 3 }

    val average: Float? get() {
        val weighted = grades.filter { !it.isVerbal && it.gradeNumeric != null && it.importance != null }
        if (weighted.isEmpty()) return null
        val sumWeights = weighted.sumOf { it.importance!!.toDouble() }
        if (sumWeights == 0.0) return null
        return (weighted.sumOf { (it.gradeNumeric!! * it.importance!!).toDouble() } / sumWeights).toFloat()
    }
}

object GradesApi {

    suspend fun fetchGrades(term: String? = null, year: Int? = null): List<SubjectGrades>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val url = if (term != null && year != null)
                "https://${EduSession.subdomain}.edupage.org/znamky/" +
                    "?what=studentviewer&znamky_yearid=$year&nadobdobie=$term"
            else
                "https://${EduSession.subdomain}.edupage.org/znamky/"

            val html = EduHttp.get(url, "EduGrades") ?: return@withContext null

            parseGradesHtml(html)
        }.getOrElse { e ->
            Log.e("EduGrades", "fetchGrades: ${e.message}", e)
            null
        }
    }

    suspend fun fetchAvailableYears(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val html = EduHttp.get(
                "https://${EduSession.subdomain}.edupage.org/znamky/",
                "EduGrades"
            ) ?: return@withContext fallbackYears()

            val regex = Regex("""<option[^>]*value="(\d{4})"[^>]*>\s*(\d{4}/\d{4})\s*</option>""")
            val matches = regex.findAll(html).map {
                it.groupValues[1].toInt() to it.groupValues[2]
            }.toList()

            matches.ifEmpty { fallbackYears() }
        }.getOrElse { fallbackYears() }
    }

    private fun parseGradesHtml(html: String): List<SubjectGrades> {
        val jsonStr = html
            .split(".znamkyStudentViewer(").getOrNull(1)
            ?.split(");\r\n\t\t});\r\n\t\t</script>")?.getOrNull(0)
            ?: html.split(".znamkyStudentViewer(").getOrNull(1)
                ?.split(");\n\t\t});\n\t\t</script>")?.getOrNull(0)
            ?: html.split(".znamkyStudentViewer(").getOrNull(1)
                ?.let { raw -> raw.substring(0, raw.lastIndexOf(");")) }

        if (jsonStr == null) {
            Log.e("EduGrades", "parseGradesHtml: could not find .znamkyStudentViewer marker. " +
                "HTML head: ${html.take(500)}")
            return emptyList()
        }

        val data      = JSONObject(jsonStr)
        val rawGrades = data.optJSONArray("vsetkyZnamky") ?: return emptyList()
        val vsetkyUdalosti = data.optJSONObject("vsetkyUdalosti")
        val gradeEvents = mutableMapOf<String, JSONObject>()
        if (vsetkyUdalosti != null) {
            val outerKeys = vsetkyUdalosti.keys()
            while (outerKeys.hasNext()) {
                val outerKey = outerKeys.next()
                val inner = vsetkyUdalosti.optJSONObject(outerKey) ?: continue
                val innerKeys = inner.keys()
                while (innerKeys.hasNext()) {
                    val eventId = innerKeys.next()
                    if (!gradeEvents.containsKey(eventId)) {
                        gradeEvents[eventId] = inner.getJSONObject(eventId)
                    }
                }
            }
        }

        Log.d("EduGrades", "vsetkyUdalosti keys: ${vsetkyUdalosti?.keys()?.asSequence()?.toList()}, " +
            "total events: ${gradeEvents.size}, total grades: ${rawGrades.length()}")

        val parsed = mutableListOf<EduGrade>()
        var skippedNoEvent  = 0
        var skippedNoSubject = 0

        for (i in 0 until rawGrades.length()) {
            val g       = rawGrades.getJSONObject(i)
            val eventId = g.optString("udalostid")
            if (eventId.isBlank()) continue
            val details = gradeEvents[eventId]
            if (details == null) {
                skippedNoEvent++
                Log.w("EduGrades", "skip[$i] event '$eventId' not in gradeEvents | data=${g.optString("data")}")
                continue
            }

            // When udalostid="0" the event is a catch-all "Nezaradené" entry.
            // The actual subject ID lives directly on the grade object as "predmetid".
            val subjectIdStr = details.optString("PredmetID")
                .ifBlank { null }
                ?.takeIf { it != "vsetky" }
                ?: g.optString("predmetid").ifBlank { null }

            if (subjectIdStr == null) {
                skippedNoSubject++
                Log.w("EduGrades", "skip[$i] no subject ID anywhere, event=$eventId data=${g.optString("data")}")
                continue
            }

            val gradeType = details.optString("p_typ_udalosti").toIntOrNull() ?: 1

            val maxPoints: Float?
            val importance: Float?
            when (gradeType) {
                1 -> { maxPoints = null; importance = details.optString("p_vaha").toFloatOrNull()?.div(20f) }
                2 -> { maxPoints = details.optString("p_vaha").toFloatOrNull(); importance = null }
                3 -> { maxPoints = details.optString("p_vaha_body").toFloatOrNull()
                       importance = details.optString("p_vaha").toFloatOrNull()?.div(20f) }
                else -> { maxPoints = null; importance = null }
            }

            val rawData    = g.optString("data")
            val gradeParts = rawData.split(" (", limit = 2)
            val gradeValue = gradeParts[0].trim()

            val gradeNumeric = gradeValue.toFloatOrNull()
            val isVerbal     = gradeNumeric == null

            val percent: Float? = if (!isVerbal && maxPoints != null && maxPoints > 0f)
                (gradeNumeric!! / maxPoints * 100f) else null

            val classAvg    = details.optString("priemer").toFloatOrNull()
            val teacherId = (details.optString("UcitelID").ifBlank { null }
                ?: g.optString("ucitelid").ifBlank { null }
                    ?.takeIf { it != "-7" })  // -7 = system/auto entry
            val teacherName = teacherId?.let { EduSession.teacherName(it) } ?: ""

            // For catch-all events (udalostid=0), p_meno is "Nezaradené", use a
            // friendlier fallback title derived from the date instead.
            val title = details.optString("p_meno")
                .ifBlank { null }
                ?.takeIf { it != "Nezaradené" }
                ?: g.optString("datum").take(10).ifBlank { "Grade" }

            parsed.add(EduGrade(
                eventId      = eventId,
                title        = title,
                gradeValue   = gradeValue,
                gradeNumeric = gradeNumeric,
                date         = g.optString("datum").take(10),
                subjectId    = subjectIdStr,
                subjectName  = EduSession.subjectName(subjectIdStr),
                subjectShort = EduSession.subjectShort(subjectIdStr),
                teacherName  = teacherName,
                maxPoints    = maxPoints,
                importance   = importance,
                percent      = percent,
                classAvg     = classAvg,
                isVerbal     = isVerbal,
                gradeType    = gradeType,
            ))
        }

        Log.d("EduGrades", "parsed=${parsed.size} skippedNoEvent=$skippedNoEvent skippedNoSubject=$skippedNoSubject")
        val subjectSummary = parsed.groupBy { it.subjectId }
            .map { (id, gs) -> "${gs.first().subjectShort}($id)×${gs.size}" }
        Log.d("EduGrades", "subjects found: $subjectSummary")

        return parsed
            .groupBy { it.subjectId }
            .map { (id, grades) ->
                SubjectGrades(
                    subjectId    = id,
                    subjectName  = grades.first().subjectName,
                    subjectShort = grades.first().subjectShort,
                    grades       = grades.sortedByDescending { it.date },
                )
            }
    }

    private fun fallbackYears(): List<Pair<Int, String>> {
        val today     = LocalDate.now()
        val startYear = if (today.monthValue >= 9) today.year else today.year - 1
        return (0..8).map { offset -> val y = startYear - offset; y to "$y/${y + 1}" }
    }
}
