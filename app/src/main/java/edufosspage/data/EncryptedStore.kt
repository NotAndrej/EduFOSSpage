package org.edufosspage.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

object EncryptedStore {

    private const val TAG = "EncryptedStore"

    private fun masterKey(ctx: Context): MasterKey =
        MasterKey.Builder(ctx.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun encryptedFile(ctx: Context, file: File): EncryptedFile =
        EncryptedFile.Builder(
            ctx.applicationContext,
            file,
            masterKey(ctx),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()

    fun read(ctx: Context, file: File): String? {
        if (!file.exists()) return null
        return runCatching {
            encryptedFile(ctx, file).openFileInput().use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull()
    }

    fun write(ctx: Context, file: File, content: String) {
        // EncryptedFile cannot overwrite - delete first
        if (file.exists()) file.delete()
        runCatching {
            encryptedFile(ctx, file).openFileOutput().use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        }.onFailure {
            Log.e(TAG, "write ${file.name}: ${it.message}")
            throw it
        }
    }
}
