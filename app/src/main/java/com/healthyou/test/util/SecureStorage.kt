package com.healthyou.test.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper around EncryptedSharedPreferences to store small secrets securely.
 *
 * Usage:
 * val storage = SecureStorage(context)
 * storage.saveAuthToken("my-client-token")
 * val token = storage.getAuthToken()
 */
class SecureStorage(context: Context) {

    private val prefsName = "healthyou_secure_prefs_v1"
    private val keyAuthToken = "auth_token"

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        prefsName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        prefs.edit().putString(keyAuthToken, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(keyAuthToken, null)

    fun clearAuthToken() {
        prefs.edit().remove(keyAuthToken).apply()
    }
}
