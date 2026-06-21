package org.edufosspage.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.edufosspage.R
import org.edufosspage.data.LoginResult
import org.edufosspage.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
class NotificationWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "NotifWorker"

        // Notification IDs - stable per-channel so Android collapses them.
        private const val NID_MESSAGES  = 1001
        private const val NID_GRADES    = 1002
        private const val NID_TIMETABLE = 1003

        // Extra key written to the tap intent so MainActivity knows which tab to open.
        const val EXTRA_TAB = "open_tab"
    }

    // Required for setExpedited() - shows a silent transient notification while the job runs
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notif = NotificationCompat.Builder(ctx, NotificationChannels.MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle("Checking for updates…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        return ForegroundInfo(9999, notif)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!NotificationStore.isMasterEnabled(ctx)) return@withContext Result.success()

        // 1. Restore or re-establish a session
        val sessionOk = ensureSession()
        if (!sessionOk) {
            Log.w(TAG, "Could not establish session - skipping poll")
            return@withContext Result.retry()
        }

        // 2. Poll each enabled channel
        if (NotificationStore.isMessagesEnabled(ctx))  checkMessages()
        if (NotificationStore.isGradesEnabled(ctx))    checkGrades()
        if (NotificationStore.isTimetableEnabled(ctx)) checkTimetable()

        Result.success()
    }

    // Session management

    private suspend fun ensureSession(): Boolean {
        // Try cached session first (fastest path).
        if (AppCache.loadSession(ctx) && EduSession.subdomain.isNotBlank()) {
            Log.d(TAG, "Session restored from cache for ${EduSession.subdomain}")
            return true
        }

        // Fall back to full re-login using saved credentials.
        val creds = CredentialStore.load(ctx) ?: run {
            Log.w(TAG, "No credentials saved")
            return false
        }

        return when (val result = EdupageLogin.login(creds.first, creds.second)) {
            is LoginResult.Success -> {
                EduApi.initSession(result.schoolUrl, result.cookies)
                AppCache.saveSession(ctx)
                Log.d(TAG, "Re-logged in successfully")
                true
            }
            else -> {
                Log.w(TAG, "Re-login failed: $result")
                false
            }
        }
    }

    // Messages

    private suspend fun checkMessages() {
        val messages = runCatching { MessagesApi.fetchMessages(daysBack = 3) }.getOrNull()
            ?: return

        val ids   = messages.map { it.id }
        val fresh = NotificationStore.filterAndMarkSeen(ctx, NotificationStore.MESSAGES_KEY, ids)

        if (fresh.isEmpty()) return

        val freshMessages = messages.filter { it.id in fresh }
        val (title, body) = when {
            freshMessages.size == 1 -> {
                val m = freshMessages.first()
                "New message from ${m.sender}" to m.preview.take(120)
            }
            else -> "${freshMessages.size} new messages" to
                freshMessages.take(3).joinToString("\n") { "• ${it.sender}: ${it.preview.take(60)}" }
        }

        postNotification(
            id      = NID_MESSAGES,
            channel = NotificationChannels.MESSAGES,
            title   = title,
            body    = body,
            tab     = DeepLinkState.TAB_MESSAGES,
        )
    }


    private suspend fun checkGrades() {
        val subjects = runCatching { GradesApi.fetchGrades() }.getOrNull()
            ?: return

        val allGrades = subjects.flatMap { it.grades }
        val ids       = allGrades.map { it.eventId }
        val fresh     = NotificationStore.filterAndMarkSeen(ctx, NotificationStore.GRADES_KEY, ids)

        if (fresh.isEmpty()) return

        val freshGrades = allGrades.filter { it.eventId in fresh }
        val (title, body) = when {
            freshGrades.size == 1 -> {
                val g = freshGrades.first()
                "New grade in ${g.subjectName}" to buildString {
                    append(g.gradeValue)
                    if (g.title.isNotBlank()) append(" - ${g.title}")
                    g.classAvg?.let { append(" (class avg: ${"%.1f".format(it)})" ) }
                }
            }
            else -> "${freshGrades.size} new grades" to
                freshGrades.take(4).joinToString("\n") {
                    "• ${it.subjectShort}: ${it.gradeValue}"
                }
        }

        postNotification(
            id      = NID_GRADES,
            channel = NotificationChannels.GRADES,
            title   = title,
            body    = body,
            tab     = DeepLinkState.TAB_GRADES,
        )
    }



    private suspend fun checkTimetable() {
        val today    = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val changed = mutableListOf<String>()

        for (date in listOf(today, tomorrow)) {
            val lessons: List<Lesson> = runCatching {
                EduApi.fetchTimetable(date)
            }.getOrNull() ?: continue

            val changedLessons: List<Lesson> = lessons.filter { l ->
                l.isCancelled || l.isSubstitution
            }

            val ids = changedLessons.map { l: Lesson ->
                val type = if (l.isCancelled) "cancelled" else "sub"
                "$date-${l.period}-${l.subjectShort}-$type"
            }

            val fresh = NotificationStore.filterAndMarkSeen(ctx, NotificationStore.TIMETABLE_KEY, ids)
            if (fresh.isEmpty()) continue

            val freshLessons: List<Lesson> = changedLessons.filter { l: Lesson ->
                val type = if (l.isCancelled) "cancelled" else "sub"
                "$date-${l.period}-${l.subjectShort}-$type" in fresh
            }

            val dayLabel = if (date == today) "Today" else "Tomorrow"
            freshLessons.forEach { l: Lesson ->
                val type = if (l.isCancelled) "cancelled" else "substitution"
                changed.add("$dayLabel - ${l.subjectName} (period ${l.period}) $type")
            }
        }

        if (changed.isEmpty()) return

        val (title, body) = when {
            changed.size == 1 -> "Timetable change" to changed.first()
            else              -> "${changed.size} timetable changes" to changed.take(4).joinToString("\n") { "• $it" }
        }

        postNotification(
            id      = NID_TIMETABLE,
            channel = NotificationChannels.TIMETABLE,
            title   = title,
            body    = body,
            tab     = DeepLinkState.TAB_TIMETABLE,
        )
    }



    private fun tapIntent(tab: Int): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB, tab)
        }
        return PendingIntent.getActivity(
            ctx, tab,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun postNotification(
        id: Int, channel: String, title: String, body: String, tab: Int,
    ) {
        val notif: Notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent(tab))
            .build()

        runCatching {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        }.onFailure {
            // SecurityException on Android 13+ if POST_NOTIFICATIONS not granted - safe to ignore
            Log.w(TAG, "Could not post notification: ${it.message}")
        }
    }
}
