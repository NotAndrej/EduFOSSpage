package org.edufosspage.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }

object ThemeStore {
    private val THEME_KEY = stringPreferencesKey("theme")

    fun themeFlow(ctx: Context): Flow<AppTheme> = ctx.appDataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT"  -> AppTheme.LIGHT
            "DARK"   -> AppTheme.DARK
            "AMOLED" -> AppTheme.AMOLED
            else     -> AppTheme.SYSTEM
        }
    }

    suspend fun setTheme(ctx: Context, theme: AppTheme) {
        ctx.appDataStore.edit { it[THEME_KEY] = theme.name }
    }
}
