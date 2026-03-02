package com.pharos.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pharos_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _hasApiKey = MutableStateFlow(getApiKey() != null)
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun getApiKey(): String? {
        return securePrefs.getString(KEY_API_KEY, null)
    }

    fun saveApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
        _hasApiKey.value = true
    }

    fun deleteApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
        _hasApiKey.value = false
    }

    fun getOnlyChangedFiles(): Boolean {
        return securePrefs.getBoolean(KEY_ONLY_CHANGED, true)
    }

    fun setOnlyChangedFiles(value: Boolean) {
        securePrefs.edit().putBoolean(KEY_ONLY_CHANGED, value).apply()
    }

    fun getApiProviderType(): String {
        return securePrefs.getString(KEY_API_PROVIDER, PROVIDER_PERPLEXITY) ?: PROVIDER_PERPLEXITY
    }

    fun setApiProviderType(provider: String) {
        securePrefs.edit().putString(KEY_API_PROVIDER, provider).apply()
    }

    companion object {
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_ONLY_CHANGED = "only_changed_files"
        private const val KEY_API_PROVIDER = "api_provider"
        const val PROVIDER_PERPLEXITY = "perplexity"
    }
}
