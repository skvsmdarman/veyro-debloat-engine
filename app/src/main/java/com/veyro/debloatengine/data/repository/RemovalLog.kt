package com.veyro.debloatengine.data.repository

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persisted log of packages removed by the engine.
 * Every debloated package is recorded so the 1-click
 * "Restore All" engine can bring it back exactly as it was.
 */
object RemovalLog {

    @Serializable
    data class Entry(
        val packageName: String,
        val appName: String,
        val category: String,
        val timestamp: Long,
        val method: String = "uninstall"
    )

    private const val PREFS = "veyro_removal_log"
    private const val KEY = "entries"

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Entry> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Entry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, entry: Entry) {
        val updated = load(context).filterNot { it.packageName == entry.packageName } + entry
        prefs(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }

    fun remove(context: Context, packageName: String) {
        val updated = load(context).filterNot { it.packageName == packageName }
        prefs(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
