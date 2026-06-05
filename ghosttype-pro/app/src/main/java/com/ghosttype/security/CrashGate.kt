package com.ghosttype.security

import android.content.Context
import com.ghosttype.BuildConfig
import com.ghosttype.utils.SettingsStore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches the crash Pastebin URL which acts as an ALLOW LIST:
 * - Versions listed in `crash_versions` → run normally.
 * - Versions NOT listed        → a high remote version is saved,
 *   triggering the Force Update screen on next approval check.
 * - `crash_app: true`           → emergency kill EVERY version.
 * - `crash_app_remove: true`    → clears the kill flag.
 */
internal object CrashGate {

    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun check(ctx: Context) {
        val prefs = SettingsStore.prefs(ctx)

        val urlStr = Obf.decode(ctx, ObfConstants.CRASH_URL)
        if (!urlStr.startsWith("https://")) return

        try {
            val req = Request.Builder().url(urlStr)
                .header("Accept", "application/json")
                .header("User-Agent", "GhostTypePro")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val body = resp.body?.string() ?: return
                val trimmed = body.trimStart()
                if (!trimmed.startsWith("{")) return
                val root = JSONObject(body)

                // Global emergency kill
                if (root.optBoolean("crash_app_remove", false)) {
                    prefs.edit().remove("crash_app_triggered").apply()
                }
                if (root.optBoolean("crash_app", false)) {
                    prefs.edit().putBoolean("crash_app_triggered", true).apply()
                    return
                }

                // Allow-list: versions in crash_versions run fine
                val versions = root.optJSONArray("crash_versions") ?: return
                val currentVer = BuildConfig.VERSION_NAME
                var allowed = false
                for (i in 0 until versions.length()) {
                    val entry = versions.optString(i, "")
                    if (entry == "*" || currentVer == entry) {
                        allowed = true
                        break
                    }
                }

                if (allowed) {
                    // Version is allowed → clear force-update + crash flags
                    prefs.edit()
                        .remove(SettingsStore.KEY_REMOTE_APP_VERSION)
                        .remove("crash_app_triggered")
                        .apply()
                } else {
                    // Version NOT allowed → trigger Force Update screen
                    prefs.edit()
                        .putString(SettingsStore.KEY_REMOTE_APP_VERSION, "99.99.99")
                        .remove("crash_app_triggered")
                        .apply()
                }
            }
        } catch (_: Exception) {}
    }
}
