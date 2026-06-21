package org.edufosspage.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Disk cache for EduPage data. All files are AES-256 encrypted via EncryptedStore.
 * When the cache is disabled, reads return null and writes are no-ops.
 */
object AppCache {

    private const val TAG = "AppCache"

    private const val F_SESSION   = "cache_session.json"
    private const val F_DASHBOARD = "cache_dashboard.json"
    private const val F_GRADES    = "cache_grades_"    // + "${year}_${term}.json"
    private const val F_MESSAGES  = "cache_messages.json"
    private const val F_TIMETABLE = "cache_timetable_" // + "${date}.json"

    private fun isEnabled(ctx: Context): Boolean {
        // Synchronous read - cache ops run on IO dispatcher so this is safe
        val prefs = ctx.getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("cache_enabled", true)
    }

    fun setCacheEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("cache_enabled", enabled).apply()
        if (!enabled) clearAll(ctx)
    }

    fun isCacheEnabled(ctx: Context): Boolean = isEnabled(ctx)

    private fun file(ctx: Context, name: String) = File(ctx.filesDir, name)

    private fun write(ctx: Context, name: String, json: String) = runCatching {
        EncryptedStore.write(ctx, file(ctx, name), json)
        Log.v(TAG, "wrote $name (${json.length} chars)")
    }.getOrElse { Log.e(TAG, "write $name: ${it.message}") }

    private fun read(ctx: Context, name: String): String? = runCatching {
        val f = file(ctx, name)
        if (!f.exists()) return null
        if (System.currentTimeMillis() - f.lastModified() > 24 * 3600 * 1000L) {
            Log.d(TAG, "expired: $name"); f.delete(); return null
        }
        EncryptedStore.read(ctx, f)
    }.getOrElse { Log.e(TAG, "read $name: ${it.message}"); null }

    fun clearAll(ctx: Context) {
        ctx.filesDir.listFiles()
            ?.filter { it.name.startsWith("cache_") }
            ?.forEach { it.delete() }
        Log.i(TAG, "cache cleared")
    }

    // Session

    suspend fun saveSession(ctx: Context) = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext
        val j = JSONObject().apply {
            put("schoolUrl",     EduSession.schoolUrl)
            put("subdomain",     EduSession.subdomain)
            put("gsecHash",      EduSession.gsecHash)
            put("userId",        EduSession.userId)
            put("userFullName",  EduSession.userFullName)
            put("userClass",     EduSession.userClass)
            put("cookies",       JSONArray(EduSession.cookies))
            val subjectsJ = JSONObject()
            EduSession.subjects.forEach { (id, p) ->
                subjectsJ.put(id, JSONObject().put("name", p.first).put("short", p.second))
            }
            put("subjects", subjectsJ)
            val teachersJ = JSONObject()
            EduSession.teachers.forEach { (id, n) -> teachersJ.put(id, n) }
            put("teachers", teachersJ)
            val classroomsJ = JSONObject()
            EduSession.classrooms.forEach { (id, n) -> classroomsJ.put(id, n) }
            put("classrooms", classroomsJ)
        }
        write(ctx, F_SESSION, j.toString())
    }

    suspend fun loadSession(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext false
        val text = read(ctx, F_SESSION) ?: return@withContext false
        runCatching {
            val j = JSONObject(text)
            EduSession.schoolUrl     = j.optString("schoolUrl")
            EduSession.subdomain     = j.optString("subdomain")
            EduSession.gsecHash      = j.optString("gsecHash")
            EduSession.userId        = j.optString("userId")
            EduSession.userFullName  = j.optString("userFullName")
            EduSession.userClass     = j.optString("userClass")

            val cookiesJ = j.optJSONArray("cookies")
            EduSession.cookies = (0 until (cookiesJ?.length() ?: 0))
                .map { cookiesJ!!.getString(it) }

            val subjectsJ = j.optJSONObject("subjects")
            EduSession.subjects = buildMap {
                subjectsJ?.keys()?.forEach { id ->
                    val o = subjectsJ.getJSONObject(id)
                    put(id, o.optString("name") to o.optString("short"))
                }
            }
            val teachersJ = j.optJSONObject("teachers")
            EduSession.teachers = buildMap {
                teachersJ?.keys()?.forEach { id -> put(id, teachersJ.getString(id)) }
            }
            val classroomsJ = j.optJSONObject("classrooms")
            EduSession.classrooms = buildMap {
                classroomsJ?.keys()?.forEach { id -> put(id, classroomsJ.getString(id)) }
            }
            Log.d(TAG, "session restored: ${EduSession.userFullName}")
            true
        }.getOrElse { Log.e(TAG, "loadSession: ${it.message}"); false }
    }

    // Dashboard

    suspend fun saveDashboard(
        ctx: Context,
        lessons: List<Lesson>,
        displayDate: String,
        messages: List<DashMessage>,
        grades: List<DashGrade>
    ) = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext
        val j = JSONObject().apply {
            put("displayDate", displayDate)
            put("lessons",  JSONArray(lessons.map  { lessonToJson(it) }))
            put("messages", JSONArray(messages.map { msgToJson(it) }))
            put("grades",   JSONArray(grades.map   { gradeToJson(it) }))
        }
        write(ctx, F_DASHBOARD, j.toString())
    }

    data class DashboardCache(
        val lessons:     List<Lesson>,
        val displayDate: String,
        val messages:    List<DashMessage>,
        val grades:      List<DashGrade>,
    )

    suspend fun loadDashboard(ctx: Context): DashboardCache? = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext null
        val text = read(ctx, F_DASHBOARD) ?: return@withContext null
        runCatching {
            val j = JSONObject(text)
            val lessonsArr  = j.optJSONArray("lessons")
            val messagesArr = j.optJSONArray("messages")
            val gradesArr   = j.optJSONArray("grades")
            DashboardCache(
                lessons     = (0 until (lessonsArr?.length()  ?: 0)).map { lessonFromJson(lessonsArr!!.getJSONObject(it)) },
                displayDate = j.optString("displayDate"),
                messages    = (0 until (messagesArr?.length() ?: 0)).map { msgFromJson(messagesArr!!.getJSONObject(it)) },
                grades      = (0 until (gradesArr?.length()   ?: 0)).map { gradeFromJson(gradesArr!!.getJSONObject(it)) },
            )
        }.getOrElse { Log.e(TAG, "loadDashboard: ${it.message}"); null }
    }

    // Grades

    suspend fun saveGrades(ctx: Context, term: String, year: Int, data: List<SubjectGrades>) =
    withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext
        val arr = JSONArray(data.map { s ->
            JSONObject().apply {
                put("subjectId",    s.subjectId)
                put("subjectName",  s.subjectName)
                put("subjectShort", s.subjectShort)
                put("grades", JSONArray(s.grades.map { g ->
                    JSONObject().apply {
                        put("eventId",      g.eventId)
                        put("title",        g.title)
                        put("gradeValue",   g.gradeValue)
                        put("gradeNumeric", g.gradeNumeric ?: JSONObject.NULL)
                        put("date",         g.date)
                        put("subjectId",    g.subjectId)
                        put("subjectName",  g.subjectName)
                        put("subjectShort", g.subjectShort)
                        put("teacherName",  g.teacherName)
                        put("maxPoints",    g.maxPoints  ?: JSONObject.NULL)
                        put("importance",   g.importance ?: JSONObject.NULL)
                        put("percent",      g.percent    ?: JSONObject.NULL)
                        put("classAvg",     g.classAvg   ?: JSONObject.NULL)
                        put("isVerbal",     g.isVerbal)
                        put("gradeType",    g.gradeType)
                    }
                }))
            }
        })
        write(ctx, "$F_GRADES${year}_$term.json", arr.toString())
    }

    suspend fun loadGrades(ctx: Context, term: String, year: Int): List<SubjectGrades>? =
    withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext null
        val text = read(ctx, "$F_GRADES${year}_$term.json") ?: return@withContext null
        runCatching {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val s    = arr.getJSONObject(i)
                val gArr = s.optJSONArray("grades")
                SubjectGrades(
                    subjectId    = s.optString("subjectId"),
                    subjectName  = s.optString("subjectName"),
                    subjectShort = s.optString("subjectShort"),
                    grades       = (0 until (gArr?.length() ?: 0)).map { gi ->
                        val g = gArr!!.getJSONObject(gi)
                        EduGrade(
                            eventId      = g.optString("eventId"),
                            title        = g.optString("title"),
                            gradeValue   = g.optString("gradeValue"),
                            gradeNumeric = if (g.isNull("gradeNumeric")) null else g.optDouble("gradeNumeric").toFloat(),
                            date         = g.optString("date"),
                            subjectId    = g.optString("subjectId"),
                            subjectName  = g.optString("subjectName"),
                            subjectShort = g.optString("subjectShort"),
                            teacherName  = g.optString("teacherName"),
                            maxPoints    = if (g.isNull("maxPoints"))  null else g.optDouble("maxPoints").toFloat(),
                            importance   = if (g.isNull("importance")) null else g.optDouble("importance").toFloat(),
                            percent      = if (g.isNull("percent"))    null else g.optDouble("percent").toFloat(),
                            classAvg     = if (g.isNull("classAvg"))   null else g.optDouble("classAvg").toFloat(),
                            isVerbal     = g.optBoolean("isVerbal"),
                            gradeType    = g.optInt("gradeType", 1),
                        )
                    }
                )
            }
        }.getOrElse { Log.e(TAG, "loadGrades: ${it.message}"); null }
    }

    // Messages

    suspend fun saveMessages(ctx: Context, messages: List<EduMessage>) = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext
        write(ctx, F_MESSAGES, JSONArray(messages.map { m ->
            JSONObject().apply {
                put("id",        m.id)
                put("sender",    m.sender)
                put("preview",   m.preview)
                put("timestamp", m.timestamp)
                put("date",      m.date)
                put("time",      m.time)
                put("type",      m.type)
            }
        }).toString())
    }

    suspend fun loadMessages(ctx: Context): List<EduMessage>? = withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext null
        val text = read(ctx, F_MESSAGES) ?: return@withContext null
        runCatching {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                EduMessage(
                    id        = m.optString("id"),
                    sender    = m.optString("sender"),
                    preview   = m.optString("preview"),
                    timestamp = m.optString("timestamp"),
                    date      = m.optString("date"),
                    time      = m.optString("time"),
                    type      = m.optString("type"),
                )
            }
        }.getOrElse { Log.e(TAG, "loadMessages: ${it.message}"); null }
    }

    // Timetable

    suspend fun saveTimetable(ctx: Context, date: String, lessons: List<Lesson>) =
    withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext
        write(ctx, "$F_TIMETABLE$date.json", JSONArray(lessons.map { lessonToJson(it) }).toString())
    }

    suspend fun loadTimetable(ctx: Context, date: String): List<Lesson>? =
    withContext(Dispatchers.IO) {
        if (!isEnabled(ctx)) return@withContext null
        val text = read(ctx, "$F_TIMETABLE$date.json") ?: return@withContext null
        runCatching {
            val arr = JSONArray(text)
            (0 until arr.length()).map { lessonFromJson(arr.getJSONObject(it)) }
        }.getOrElse { Log.e(TAG, "loadTimetable: ${it.message}"); null }
    }

    // Lesson serialization

    private fun lessonToJson(l: Lesson) = JSONObject().apply {
        put("period",         l.period ?: JSONObject.NULL)
        put("startTime",      l.startTime)
        put("endTime",        l.endTime)
        put("subjectName",    l.subjectName)
        put("subjectShort",   l.subjectShort)
        put("teacherName",    l.teacherName)
        put("classroom",      l.classroom)
        put("groups",         JSONArray(l.groups))
        put("isCancelled",    l.isCancelled)
        put("isSubstitution", l.isSubstitution)
        put("curriculum",     l.curriculum ?: JSONObject.NULL)
    }

    private fun lessonFromJson(j: JSONObject) = Lesson(
        period         = if (j.isNull("period")) null else j.optInt("period"),
        startTime      = j.optString("startTime"),
        endTime        = j.optString("endTime"),
        subjectName    = j.optString("subjectName"),
        subjectShort   = j.optString("subjectShort"),
        teacherName    = j.optString("teacherName"),
        classroom      = j.optString("classroom"),
        groups         = j.optJSONArray("groups")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        isCancelled    = j.optBoolean("isCancelled"),
        isSubstitution = j.optBoolean("isSubstitution"),
        curriculum     = if (j.isNull("curriculum")) null else j.optString("curriculum"),
    )

    private fun msgToJson(m: DashMessage) = JSONObject().apply {
        put("sender",  m.sender); put("preview", m.preview); put("timeAgo", m.timeAgo)
    }
    private fun msgFromJson(j: JSONObject) =
        DashMessage(j.optString("sender"), j.optString("preview"), j.optString("timeAgo"))

    private fun gradeToJson(g: DashGrade) = JSONObject().apply {
        put("subject", g.subject); put("grade", g.grade); put("date", g.date)
    }
    private fun gradeFromJson(j: JSONObject) =
        DashGrade(j.optString("subject"), j.optString("grade"), j.optString("date"))
}

// Shared state flag - true while any screen is showing cached data pending a refresh

object AppCacheState {
    private val _showingCached = MutableStateFlow(false)
    val showingCached = _showingCached.asStateFlow()

    private var cachedCount = 0

    @Synchronized fun markCached()  { cachedCount++; _showingCached.value = true }
    @Synchronized fun markFresh()   { if (cachedCount > 0) cachedCount--; if (cachedCount == 0) _showingCached.value = false }
    @Synchronized fun markOffline() { _showingCached.value = true }
}
