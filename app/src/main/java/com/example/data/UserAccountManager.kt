package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class UserAccount(
    val uid: String = "guest_device",
    val email: String = "",
    val displayName: String = "Guest User",
    val isSignedIn: Boolean = false,
    val isCloudSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val syncedFavoritesCount: Int = 0,
    val syncedCallLogsCount: Int = 0,
    val syncedSpeedDialsCount: Int = 0
)

class UserAccountRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("streetrise_account_prefs", Context.MODE_PRIVATE)

    private val _account = MutableStateFlow(loadAccount())
    val account: StateFlow<UserAccount> = _account.asStateFlow()

    private fun loadAccount(): UserAccount {
        val isSignedIn = prefs.getBoolean("is_signed_in", false)
        if (!isSignedIn) {
            return UserAccount(
                uid = "guest_${android.os.Build.MODEL.hashCode().toString().take(6)}",
                email = "",
                displayName = "Guest Member",
                isSignedIn = false,
                isCloudSyncEnabled = false,
                lastSyncTimestamp = prefs.getLong("last_sync_timestamp", 0L),
                syncedFavoritesCount = prefs.getInt("synced_favorites_count", 0),
                syncedCallLogsCount = prefs.getInt("synced_call_logs_count", 0),
                syncedSpeedDialsCount = prefs.getInt("synced_speed_dials_count", 0)
            )
        }

        return UserAccount(
            uid = prefs.getString("uid", "user_default") ?: "user_default",
            email = prefs.getString("email", "member@streetrise.org") ?: "member@streetrise.org",
            displayName = prefs.getString("display_name", "Community Member") ?: "Community Member",
            isSignedIn = true,
            isCloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", true),
            lastSyncTimestamp = prefs.getLong("last_sync_timestamp", System.currentTimeMillis()),
            syncedFavoritesCount = prefs.getInt("synced_favorites_count", 0),
            syncedCallLogsCount = prefs.getInt("synced_call_logs_count", 0),
            syncedSpeedDialsCount = prefs.getInt("synced_speed_dials_count", 0)
        )
    }

    fun signIn(email: String, name: String) {
        val cleanEmail = email.trim().ifEmpty { "member@streetrise.org" }
        val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords() }
        val uid = "sr_user_" + cleanEmail.hashCode().toString().replace("-", "")

        prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("uid", uid)
            .putString("email", cleanEmail)
            .putString("display_name", cleanName)
            .putBoolean("cloud_sync_enabled", true)
            .putLong("last_sync_timestamp", System.currentTimeMillis())
            .apply()

        _account.value = loadAccount()
    }

    fun signOut(keepLocalData: Boolean = true) {
        prefs.edit()
            .putBoolean("is_signed_in", false)
            .putString("email", "")
            .putString("display_name", "Guest Member")
            .apply()

        _account.value = loadAccount()
    }

    fun updateSyncMetrics(favoritesCount: Int, callLogsCount: Int, dialsCount: Int) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong("last_sync_timestamp", now)
            .putInt("synced_favorites_count", favoritesCount)
            .putInt("synced_call_logs_count", callLogsCount)
            .putInt("synced_speed_dials_count", dialsCount)
            .apply()

        _account.value = _account.value.copy(
            lastSyncTimestamp = now,
            syncedFavoritesCount = favoritesCount,
            syncedCallLogsCount = callLogsCount,
            syncedSpeedDialsCount = dialsCount
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
