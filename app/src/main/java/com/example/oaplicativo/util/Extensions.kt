@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.util

import java.util.Locale

/**
 * EXTENSIONS: PADRONIZAÇÃO SÊNIOR DE DADOS
 * Unifica o "Space Hack" e tratamento de nulos em toda a base de código.
 */

/**
 * Garante que strings vazias ou nulas sejam salvas como um espaço (" ") 
 * para evitar conflitos no banco de dados e garantir compatibilidade com relatórios.
 */
fun String?.orSpace(): String = if (this.isNullOrBlank()) " " else this

/**
 * Converte o "Space Hack" de volta para nulo/vazio quando necessário para lógica de UI.
 */
fun String?.ifSpaceNull(): String? = if (this == " " || this.isNullOrBlank()) null else this

/**
 * Normaliza strings de qualidade para garantir que batam com as chaves do repositório.
 * Ex: "BOA" -> "Boa", "regular" -> "Regular"
 */
fun String?.normalizeQuality(): String {
    val base = this?.lowercase(Locale.ROOT)?.trim() ?: "ruim"
    return base.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}
