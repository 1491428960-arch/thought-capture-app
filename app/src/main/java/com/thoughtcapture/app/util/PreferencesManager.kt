package com.thoughtcapture.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "thought_capture_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var githubPat: String?
        get() = prefs.getString(KEY_PAT, null)
        set(value) = prefs.edit().putString(KEY_PAT, value).apply()

    var repoUrl: String?
        get() = prefs.getString(KEY_REPO_URL, null)
        set(value) = prefs.edit().putString(KEY_REPO_URL, value).apply()

    var repoBranch: String
        get() = prefs.getString(KEY_BRANCH, "main") ?: "main"
        set(value) = prefs.edit().putString(KEY_BRANCH, value).apply()

    var syncOnMobileData: Boolean
        get() = prefs.getBoolean(KEY_MOBILE_DATA, false)
        set(value) = prefs.edit().putBoolean(KEY_MOBILE_DATA, value).apply()

    val isConfigured: Boolean
        get() = !githubPat.isNullOrEmpty() && !repoUrl.isNullOrEmpty()

    companion object {
        private const val KEY_PAT = "github_pat"
        private const val KEY_REPO_URL = "github_repo_url"
        private const val KEY_BRANCH = "github_branch"
        private const val KEY_MOBILE_DATA = "sync_mobile_data"
    }
}
