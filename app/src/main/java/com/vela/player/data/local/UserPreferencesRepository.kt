package com.vela.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vela.player.player.PlaybackQueueMode
import com.vela.player.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.velaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vela_player_preferences"
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultQuality: String = "720",
    val igLoggedIn: Boolean = false,
    val lastYtdlpUpdateMs: Long = 0L,
    val queueMode: PlaybackQueueMode = PlaybackQueueMode.IN_ORDER
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val IG_LOGGED_IN = booleanPreferencesKey("ig_logged_in")
        val LAST_YTDLP_UPDATE_MS = longPreferencesKey("last_ytdlp_update_ms")
        val QUEUE_MODE = stringPreferencesKey("queue_mode")
    }

    val settings: Flow<UserSettings> = context.velaDataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromStored(prefs[Keys.THEME_MODE]),
            defaultQuality = prefs[Keys.DEFAULT_QUALITY] ?: "720",
            igLoggedIn = prefs[Keys.IG_LOGGED_IN] ?: false,
            lastYtdlpUpdateMs = prefs[Keys.LAST_YTDLP_UPDATE_MS] ?: 0L,
            queueMode = PlaybackQueueMode.fromStored(prefs[Keys.QUEUE_MODE])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.velaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.velaDataStore.edit { it[Keys.DEFAULT_QUALITY] = quality }
    }

    suspend fun setIgLoggedIn(loggedIn: Boolean) {
        context.velaDataStore.edit { it[Keys.IG_LOGGED_IN] = loggedIn }
    }

    suspend fun setLastYtdlpUpdateMs(timestampMs: Long) {
        context.velaDataStore.edit { it[Keys.LAST_YTDLP_UPDATE_MS] = timestampMs }
    }

    suspend fun setQueueMode(mode: PlaybackQueueMode) {
        context.velaDataStore.edit { it[Keys.QUEUE_MODE] = mode.name }
    }
}
