package com.pharos.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val securePrefs: SharedPreferences

    init {
        securePrefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback: if encrypted prefs fail (corrupted keystore, first-run race, etc.),
            // delete the file and retry once
            Log.w(TAG, "EncryptedSharedPreferences init failed, retrying", e)
            try {
                val file = java.io.File(
                    context.filesDir.parent, "shared_prefs/$PREFS_FILE.xml"
                )
                if (file.exists()) file.delete()

                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences retry failed, using plain prefs", e2)
                context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private val _hasApiKey = MutableStateFlow(getApiKey() != null)
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun getApiKey(): String? {
        return try {
            securePrefs.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read API key", e)
            null
        }
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
        private const val TAG = "SettingsRepository"
        private const val PREFS_FILE = "pharos_secure_prefs"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_ONLY_CHANGED = "only_changed_files"
        private const val KEY_API_PROVIDER = "api_provider"
        const val PROVIDER_PERPLEXITY = "perplexity"
    }
}
