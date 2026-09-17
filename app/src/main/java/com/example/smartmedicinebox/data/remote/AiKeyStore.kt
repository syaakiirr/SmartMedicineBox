@file:Suppress("DEPRECATION")

package com.example.smartmedicinebox.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AiKeyStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        if (preferences.contains(LEGACY_OPENAI_KEY) || preferences.contains(LEGACY_PRIVACY_ACCEPTED)) {
            preferences.edit()
                .remove(LEGACY_OPENAI_KEY)
                .remove(LEGACY_PRIVACY_ACCEPTED)
                .apply()
        }
    }

    fun hasKey(): Boolean = apiKey().isNotBlank()

    fun hasAcceptedPrivacy(): Boolean = preferences.getBoolean(KEY_PRIVACY_ACCEPTED, false)

    fun apiKey(): String = preferences.getString(KEY_API_KEY, "").orEmpty()

    fun saveKey(apiKey: String) {
        preferences.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun clearKey() {
        preferences.edit().remove(KEY_API_KEY).apply()
    }

    fun acceptPrivacyNotice() {
        preferences.edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).apply()
    }

    companion object {
        const val PREFERENCES = "ai_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_PRIVACY_ACCEPTED = "gemini_privacy_notice_accepted"
        private const val LEGACY_OPENAI_KEY = "openai_api_key"
        private const val LEGACY_PRIVACY_ACCEPTED = "privacy_notice_accepted"
    }
}
