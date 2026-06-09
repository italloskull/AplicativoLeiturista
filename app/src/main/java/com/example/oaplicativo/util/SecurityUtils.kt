package com.example.oaplicativo.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * SECURITY UTILS: PROTOCOLO DE PERSISTÊNCIA INQUEBRÁVEL (V6)
 * Foco total em compatibilidade com Xiaomi/MIUI e garantia de escrita no disco.
 */
object SecurityUtils {
    // Mudança de nome para V6 para garantir que o Android ignore qualquer cache corrompido anterior
    private const val PREFS_NAME = "unbreakable_auth_v7"
    private const val KEY_IDENTIFIER = "persistent_user"
    private const val KEY_PASSWORD = "persistent_pass"
    private const val KEY_REMEMBER_ME = "is_remember_enabled"

    /**
     * Retorna o banco de dados de preferências padrão com Modo Privado.
     * SÊNIOR DECISION: Removemos a biblioteca 'androidx.security' temporariamente para resolver 
     * a incompatibilidade física com aparelhos Xiaomi que estão rejeitando o Keystore.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * SALVAMENTO ATÔMICO: Grava os dados e aguarda a confirmação física do hardware (Disk Write).
     */
    fun saveCredentials(context: Context, identifier: String, pass: String, remember: Boolean) {
        try {
            val editor = getPrefs(context).edit()
            
            // Sempre limpamos antes para garantir que não haja lixo
            editor.clear() 

            if (remember) {
                editor.putBoolean(KEY_REMEMBER_ME, true)
                editor.putString(KEY_IDENTIFIER, identifier.trim().lowercase())
                editor.putString(KEY_PASSWORD, pass)
            } else {
                editor.putBoolean(KEY_REMEMBER_ME, false)
            }
            
            // SÊNIOR FIX: .commit() é síncrono e retorna booleano. .apply() é assíncrono.
            // Para login, PRECISAMOS da garantia de escrita agora.
            val success = editor.commit()
            Log.i("SecurityUtils", "🔒 Persistência V6 - Sucesso: $success | Usuário: $identifier | Remember: $remember")
            
        } catch (e: Exception) {
            Log.e("SecurityUtils", "❌ Erro Crítico de Disco", e)
        }
    }

    fun getRememberedIdentifier(context: Context): String? {
        val value = getPrefs(context).getString(KEY_IDENTIFIER, null)
        Log.d("SecurityUtils", "📖 Lendo Usuário: $value")
        return value
    }

    fun getRememberedPassword(context: Context): String? {
        return getPrefs(context).getString(KEY_PASSWORD, null)
    }

    fun isRememberMeEnabled(context: Context): Boolean {
        val enabled = getPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
        Log.d("SecurityUtils", "📖 Lembrar está ativo? $enabled")
        return enabled
    }

    /**
     * Limpa fisicamente o arquivo do disco.
     */
    fun clearCredentials(context: Context) {
        getPrefs(context).edit().clear().commit()
        Log.w("SecurityUtils", "🧹 Cofre limpo pelo usuário.")
    }
}
