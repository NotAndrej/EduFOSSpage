package org.edufosspage.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// DataStore instance shared by BiometricStore and ThemeStore
val Context.appDataStore by preferencesDataStore(name = "settings")

object BiometricStore {
    private val ENABLED_KEY         = booleanPreferencesKey("biometric_enabled")
    private val TIMEOUT_KEY         = intPreferencesKey("biometric_timeout_minutes")
    private val LAST_BACKGROUND_KEY = longPreferencesKey("biometric_last_background_ms")

    fun enabledFlow(ctx: Context): Flow<Boolean> =
        ctx.appDataStore.data.map { it[ENABLED_KEY] ?: false }

    fun timeoutFlow(ctx: Context): Flow<Int> =
        ctx.appDataStore.data.map { it[TIMEOUT_KEY] ?: 0 }

    fun lastBackgroundFlow(ctx: Context): Flow<Long> =
        ctx.appDataStore.data.map { it[LAST_BACKGROUND_KEY] ?: 0L }

    suspend fun setEnabled(ctx: Context, enabled: Boolean) =
        ctx.appDataStore.edit { it[ENABLED_KEY] = enabled }

    suspend fun setTimeout(ctx: Context, minutes: Int) =
        ctx.appDataStore.edit { it[TIMEOUT_KEY] = minutes }

    suspend fun setLastBackground(ctx: Context, epochMs: Long) =
        ctx.appDataStore.edit { it[LAST_BACKGROUND_KEY] = epochMs }
}

object PinStore {
    private const val PREFS        = "pin_prefs"
    private const val KEY_HASH     = "pin_hash"
    private const val KEY_ENABLED  = "pin_enabled"
    private const val KEY_ATTEMPTS = "pin_attempts"

    // PBKDF2 params - 120k rounds is ~100ms on a mid-range phone
    private const val PBKDF2_ITERATIONS = 120_000
    private const val SALT_BYTES        = 16
    private const val HASH_BITS         = 256

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun hasPin(ctx: Context): Boolean    = prefs(ctx).getString(KEY_HASH, null) != null
    fun getAttempts(ctx: Context): Int    = prefs(ctx).getInt(KEY_ATTEMPTS, 0)
    fun setAttempts(ctx: Context, n: Int) = prefs(ctx).edit().putInt(KEY_ATTEMPTS, n).apply()
    fun clearAttempts(ctx: Context)       = prefs(ctx).edit().remove(KEY_ATTEMPTS).apply()

    fun setPin(ctx: Context, pin: String) {
        prefs(ctx).edit()
            .putString(KEY_HASH, hashPin(pin))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun verify(ctx: Context, input: String): Boolean {
        val stored = prefs(ctx).getString(KEY_HASH, null) ?: return false
        // Old unsalted SHA-256 format has no ":" - clear it and force re-setup
        if (!stored.contains(":")) { clearPin(ctx); return false }
        return verifyPin(input, stored)
    }

    fun clearPin(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_HASH)
            .remove(KEY_ATTEMPTS)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    fun setEnabled(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_ENABLED, value).apply()

    // Crypto - "saltHex:hashHex" format using PBKDF2-HMAC-SHA256

    private fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "${salt.toHex()}:${pbkdf2(pin, salt).toHex()}"
    }

    private fun verifyPin(input: String, stored: String): Boolean = runCatching {
        val (saltHex, hashHex) = stored.split(":", limit = 2)
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        pbkdf2(input, salt).toHex() == hashHex
    }.getOrElse { false }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec    = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
