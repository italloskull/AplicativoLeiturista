package com.example.oaplicativo.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_IDENTIFIER = "remembered_identifier"
    private const val KEY_PASSWORD = "remembered_password"
    private const val KEY_REMEMBER_ME = "remember_me"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(context: Context, identifier: String, pass: String, rememberMe: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_IDENTIFIER, identifier)
                putString(KEY_PASSWORD, pass)
            } else {
                remove(KEY_IDENTIFIER)
                remove(KEY_PASSWORD)
            }
            apply()
        }
    }

    fun getRememberedIdentifier(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_IDENTIFIER, null)
        } else null
    }

    fun getRememberedPassword(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_PASSWORD, null)
        } else null
    }

    fun isRememberMeEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clearCredentials(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().remove(KEY_IDENTIFIER).remove(KEY_PASSWORD).apply()
    }
}