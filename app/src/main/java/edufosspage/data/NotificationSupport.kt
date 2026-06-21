package org.edufosspage.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.work.*
import java.util.concurrent.TimeUnit

// Notification Channels

object NotificationChannels {
    const val MESSAGES  = "messages"
    const val GRADES    = "grades"
    const val TIMETABLE = "timetable"

    fun createAll(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New messages from teachers and staff"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(GRADES, "Grades", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New grades and test results"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(TIMETABLE, "Timetable changes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lesson cancellations and substitutions"
            }
        )
    }
}

// Notification prefs store

object NotificationStore {
    private const val PREFS = "edufosspage_notifs"

    private const val KEY_MASTER    = "enabled"
    private const val KEY_MESSAGES  = "chan_messages"
    private const val KEY_GRADES    = "chan_grades"
    private const val KEY_TIMETABLE = "chan_timetable"

    private const val KEY_SEEN_MESSAGES  = "seen_messages"
    private const val KEY_SEEN_GRADES    = "seen_grades"
    private const val KEY_SEEN_TIMETABLE = "seen_timetable"
    private const val MAX_SEEN = 500

    val MESSAGES_KEY  get() = KEY_SEEN_MESSAGES
    val GRADES_KEY    get() = KEY_SEEN_GRADES
    val TIMETABLE_KEY get() = KEY_SEEN_TIMETABLE

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isMasterEnabled(ctx: Context)    = prefs(ctx).getBoolean(KEY_MASTER,    true)
    fun isMessagesEnabled(ctx: Context)  = prefs(ctx).getBoolean(KEY_MESSAGES,  true)
    fun isGradesEnabled(ctx: Context)    = prefs(ctx).getBoolean(KEY_GRADES,    true)
    fun isTimetableEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_TIMETABLE, true)

    fun setMaster(ctx: Context,    v: Boolean) = prefs(ctx).edit { putBoolean(KEY_MASTER,    v) }
    fun setMessages(ctx: Context,  v: Boolean) = prefs(ctx).edit { putBoolean(KEY_MESSAGES,  v) }
    fun setGrades(ctx: Context,    v: Boolean) = prefs(ctx).edit { putBoolean(KEY_GRADES,    v) }
    fun setTimetable(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_TIMETABLE, v) }

    // Ordered JSON array (not StringSet) so oldest entries are trimmed first
    private fun seen(ctx: Context, key: String): MutableList<String> {
        val json = prefs(ctx).getString(key, "[]") ?: "[]"
        return runCatching {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).mapTo(mutableListOf()) { arr.getString(it) }
        }.getOrElse { mutableListOf() }
    }

    private fun saveSeen(ctx: Context, key: String, list: MutableList<String>) {
        val trimmed = if (list.size > MAX_SEEN) list.takeLast(MAX_SEEN) else list
        prefs(ctx).edit { putString(key, org.json.JSONArray(trimmed).toString()) }
    }

    fun filterAndMarkSeen(ctx: Context, key: String, candidates: List<String>): List<String> {
        val alreadySeen = seen(ctx, key)
        val seenSet = alreadySeen.toHashSet()
        val fresh = candidates.filter { it !in seenSet }
        if (fresh.isNotEmpty()) {
            alreadySeen.addAll(fresh)
            saveSeen(ctx, key, alreadySeen)
        }
        return fresh
    }
}

// Scheduler

object NotificationScheduler {
    private const val TAG       = "NotifScheduler"
    private const val WORK_NAME = "edufosspage_poll"

    fun schedule(ctx: Context, intervalMinutes: Long = 15L) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval         = intervalMinutes,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval       = 5L,
            flexTimeIntervalUnit   = TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request,
        )
        Log.i(TAG, "Scheduled poll every $intervalMinutes min (flex 5 min)")
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        Log.i(TAG, "Poll cancelled")
    }

    // Runs a one-shot poll immediately, used when the app comes to foreground
    fun runNow(ctx: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(ctx).enqueue(request)
    }
}
