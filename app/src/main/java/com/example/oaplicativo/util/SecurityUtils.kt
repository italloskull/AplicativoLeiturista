package com.example.oaplicativo.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_IDENTIFIER = "remember_id"
    private const val KEY_PASSWORD = "remember_pass"
    private const val KEY_REMEMBER_ME = "remember_me"

    /**
     * Obtém as SharedPreferences criptografadas com sistema de Auto-Cura (Self-Healing).
     * Se houver falha de Keystore (comum em reinstalações), limpa e reinicia os dados.
     */
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Falha de Keystore detectada (AEADBadTag/KeyStore). Reiniciando cofre...", e)
            
            // OPERAÇÃO DE RESGATE: Se a chave quebrou, deletamos o arquivo corrompido para o app não travar
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                val sharedPrefsFile = java.io.File(context.filesDir.parent, "shared_prefs/$PREFS_NAME.xml")
                if (sharedPrefsFile.exists()) {
                    sharedPrefsFile.delete()
                }
            } catch (inner: Exception) {
                Log.e("SecurityUtils", "Erro ao deletar arquivo corrompido", inner)
            }
            
            // Tenta criar novamente um cofre novo e limpo
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveCredentials(context: Context, identifier: String, pass: String, remember: Boolean) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().apply {
                putBoolean(KEY_REMEMBER_ME, remember)
                if (remember) {
                    putString(KEY_IDENTIFIER, identifier)
                    putString(KEY_PASSWORD, pass)
                } else {
                    remove(KEY_IDENTIFIER)
                    remove(KEY_PASSWORD)
                }
                apply()
            }
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Erro ao salvar credenciais", e)
        }
    }

    fun getRememberedIdentifier(context: Context): String? {
        return try {
            getEncryptedPrefs(context).getString(KEY_IDENTIFIER, null)
        } catch (e: Exception) { null }
    }

    fun getRememberedPassword(context: Context): String? {
        return try {
            getEncryptedPrefs(context).getString(KEY_PASSWORD, null)
        } catch (e: Exception) { null }
    }

    fun isRememberMeEnabled(context: Context): Boolean {
        return try {
            getEncryptedPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
        } catch (e: Exception) { false }
    }

    fun clearCredentials(context: Context) {
        try {
            getEncryptedPrefs(context).edit().clear().apply()
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Erro ao limpar credenciais", e)
        }
    }
}
