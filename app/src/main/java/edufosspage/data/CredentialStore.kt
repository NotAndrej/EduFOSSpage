package org.edufosspage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object CredentialStore {

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx.applicationContext,
        "edufosspage_creds",
        MasterKey.Builder(ctx.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(ctx: Context, username: String, password: String) = runCatching {
        prefs(ctx).edit()
            .putString("username", username)
            .putString("password", password)
            .apply()
    }

    // Returns username, password or null if nothing saved/decryption fails.
    fun load(ctx: Context): Pair<String, String>? = runCatching {
        val p  = prefs(ctx)
        val u  = p.getString("username", null) ?: return@runCatching null
        val pw = p.getString("password",  null) ?: return@runCatching null
        u to pw
    }.getOrNull()

    fun clear(ctx: Context) = runCatching {
        prefs(ctx).edit().clear().apply()
    }
}
