package org.edufosspage.data

import android.content.Context

/**
 * TamperStore holds the screenshot-blocking privacy toggle (FLAG_SECURE).
 */
object TamperStore {
    private const val PREFS                 = "tamper_prefs"
    private const val KEY_SCREENSHOT_BLOCK  = "screenshot_blocked"

    fun isScreenshotBlocked(ctx: Context)           = prefs(ctx).getBoolean(KEY_SCREENSHOT_BLOCK, false)
    fun setScreenshotBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_SCREENSHOT_BLOCK, v).apply()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
