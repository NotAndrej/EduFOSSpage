package org.edufosspage.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

// Lesson model used by EduApi and AppCache

data class Lesson(
    val period:         Int?,
    val startTime:      String,
    val endTime:        String,
    val subjectName:    String,
    val subjectShort:   String,
    val teacherName:    String,
    val classroom:      String,
    val groups:         List<String>,
    val isCancelled:    Boolean,
    val isSubstitution: Boolean,
    val curriculum:     String?,
)

// Dashboard summary models

data class DashMessage(val sender: String, val preview: String, val timeAgo: String)
data class DashGrade(val subject: String, val grade: String, val date: String)

object EduApi {

    // Session init - populates EduSession from the school home page

    suspend fun initSession(schoolUrl: String, cookies: List<String>): Boolean =
    withContext(Dispatchers.IO) {
        EduSession.schoolUrl = schoolUrl
        EduSession.subdomain = URL(schoolUrl).host.split(".")[0]
        EduSession.cookies   = cookies

        val html = EduHttp.get(schoolUrl, "EduSession") ?: return@withContext false

        runCatching {
            EduSession.gsecHash = html.split("ASC.gsechash=\"")[1].split("\"")[0]

            val raw  = html.split("userhome(")[1]
            val json = JSONObject(raw.substring(0, raw.lastIndexOf(");")))

            EduSession.userId = json.optString("userid")

            val dbi = json.optJSONObject("dbi")

            EduSession.subjects = buildMap {
                dbi?.optJSONObject("subjects")?.let { s ->
                    s.keys().forEach { id ->
                        val o = s.getJSONObject(id)
                        put(id, Pair(o.optString("name"), o.optString("short")))
                    }
                }
            }
            EduSession.teachers = buildMap {
                dbi?.optJSONObject("teachers")?.let { t ->
                    t.keys().forEach { id ->
                        val o = t.getJSONObject(id)
                        put(id, "${o.optString("firstname")} ${o.optString("lastname")}".trim())
                    }
                }
            }
            EduSession.classrooms = buildMap {
                dbi?.optJSONObject("classrooms")?.let { r ->
                    r.keys().forEach { id -> put(id, r.getJSONObject(id).optString("name")) }
                }
            }

            val userIdNum = EduSession.userId
                .replace("Student", "").replace("Teacher", "").replace("Parent", "")

            val student = dbi?.optJSONObject("students")?.optJSONObject(userIdNum)
            val teacher = dbi?.optJSONObject("teachers")?.optJSONObject(userIdNum)
            val person  = student ?: teacher

            EduSession.userFullName = person?.run {
                "${optString("firstname")} ${optString("lastname")}"
            }?.trim() ?: ""

            val classId = student?.optString("classid")
            EduSession.userClass = classId?.let {
                dbi?.optJSONObject("classes")?.optJSONObject(it)?.optString("name")
            } ?: ""

            Log.d("EduSession", "User: ${EduSession.userFullName}, class: ${EduSession.userClass}")
            true
        }.getOrElse { Log.e("EduSession", "initSession: ${it.message}"); false }
    }

    // Dashboard helpers

    suspend fun fetchRecentMessages(limit: Int = 10): List<DashMessage> =
    withContext(Dispatchers.IO) {
        runCatching {
            val messages = MessagesApi.fetchMessages(daysBack = 365) ?: return@withContext emptyList()
            messages.take(limit).map { m ->
                DashMessage(sender = m.sender, preview = m.preview, timeAgo = m.date)
            }
        }.getOrElse { Log.e("EduApi", "fetchRecentMessages: ${it.message}"); emptyList() }
    }

    suspend fun fetchRecentGrades(limit: Int = 5): List<DashGrade> =
    withContext(Dispatchers.IO) {
        runCatching {
            val today = java.time.LocalDate.now()
            val term  = if (today.monthValue in 2..8) "P2" else "P1"
            val year  = if (today.monthValue >= 9) today.year else today.year - 1

            val subjects = GradesApi.fetchGrades(term, year) ?: return@withContext emptyList()
            subjects
                .flatMap { it.grades }
                .sortedByDescending { it.date }
                .take(limit)
                .map { g -> DashGrade(subject = g.subjectShort, grade = g.gradeValue, date = g.date) }
        }.getOrElse { e -> Log.e("EduApi", "fetchRecentGrades: ${e.message}"); emptyList() }
    }

    // Timetable

    suspend fun fetchTimetable(dateStr: String): List<Lesson>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val ebHtml = EduHttp.get(
                "https://${EduSession.subdomain}.edupage.org/dashboard/eb.php?mode=ttday",
                "EduTimetable"
            ) ?: return@withContext null
            val gpid = ebHtml.split("gpid=")[1].split("&")[0].trim().toInt()
            val gsh  = ebHtml.split("gsh=")[1].split("\"")[0].trim()

            val postBody = mapOf(
                "gpid"    to (gpid + 1).toString(),
                "gsh"     to gsh,
                "action"  to "loadData",
                "user"    to EduSession.userId,
                "changes" to "{}",
                "date"    to dateStr,
                "dateto"  to dateStr,
                "_LJSL"   to "4096"
            ).entries.joinToString("&") {
                "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
            }

            val response = EduHttp.post(
                "https://${EduSession.subdomain}.edupage.org/gcall",
                postBody,
                "EduTimetable"
            ) ?: return@withContext null

            val marker  = "${EduSession.userId}\","
            val jsonStr = response.substringAfter(marker).substringBeforeLast(",[")
            val json    = JSONObject(jsonStr)
            val plan    = json
                .optJSONObject("dates")
                ?.optJSONObject(dateStr)
                ?.optJSONArray("plan")
                ?: return@withContext emptyList()

            (0 until plan.length()).mapNotNull { parseLesson(plan.getJSONObject(it)) }

        }.getOrElse { e ->
            Log.e("EduTimetable", "fetchTimetable($dateStr): ${e.message}", e)
            null
        }
    }

    private fun parseLesson(l: JSONObject): Lesson? {
        val header = l.optJSONArray("header")
        if (header != null && (header.length() == 0 ||
            header.optJSONObject(0)?.optString("cmd") == "addlesson_t")) return null

        val teacherIds   = l.optJSONArray("teacherids")
        val classroomIds = l.optJSONArray("classroomids")
        val groupNames   = l.optJSONArray("groupnames")
        val hasType = l.has("type")
        val type    = if (hasType) l.optString("type") else null

        val isCancelled = l.optBoolean("removed") ||
            type == "absent" || (hasType && type == "")

        return Lesson(
            period        = l.optString("uniperiod").toIntOrNull(),
            startTime     = l.optString("starttime").replace("24:00", "23:59"),
            endTime       = l.optString("endtime").replace("24:00", "23:59"),
            subjectName   = EduSession.subjectName(l.optString("subjectid").ifEmpty { null }),
            subjectShort  = EduSession.subjectShort(l.optString("subjectid").ifEmpty { null }),
            teacherName   = (0 until (teacherIds?.length() ?: 0))
                .mapNotNull { EduSession.teacherName(teacherIds?.getString(it)).ifBlank { null } }
                .joinToString(", "),
            classroom     = (0 until (classroomIds?.length() ?: 0))
                .mapNotNull { EduSession.classroomName(classroomIds?.getString(it)).ifBlank { null } }
                .joinToString(", "),
            groups        = (0 until (groupNames?.length() ?: 0))
                .map { groupNames?.getString(it) ?: "" }.filter { it.isNotBlank() },
            isCancelled   = isCancelled,
            isSubstitution = type == "substitution",
            curriculum    = runCatching {
                l.optJSONObject("flags")?.optJSONObject("dp0")?.optString("note_wd")?.ifEmpty { null }
            }.getOrNull()
        )
    }
}
