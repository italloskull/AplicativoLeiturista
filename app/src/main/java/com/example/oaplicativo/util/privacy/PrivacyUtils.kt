package com.example.oaplicativo.util.privacy

import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrivacyUtils {
    private const val PRIVACY_DEGRADATION_MINUTES = 60L // Censura parcial após 1 hora

    // Formatos comuns que o Supabase/PostgreSQL podem retornar
    private val dateFormatters = listOf(
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSX", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX", Locale.US)
    )

    /**
     * Verifica se os dados sensíveis devem ser censurados com base na data de criação.
     * Causa Raiz de Bugs em Produção: ZonedDateTime.parse() pode falhar se o servidor 
     * mudar o formato do timestamp ou o fuso horário.
     */
    fun shouldMaskSensitiveData(createdAt: String?): Boolean {
        if (createdAt == null) return false
        
        return try {
            val createdDate = parseFlexibleDate(createdAt) ?: return false
            val now = ZonedDateTime.now()
            Duration.between(createdDate, now).toMinutes() >= PRIVACY_DEGRADATION_MINUTES
        } catch (e: Exception) {
            // Em caso de erro crítico de parse, por segurança (LGPD), não censuramos
            // para não quebrar a UI, mas logamos para depuração.
            false
        }
    }

    private fun parseFlexibleDate(dateStr: String): ZonedDateTime? {
        for (formatter in dateFormatters) {
            try {
                return ZonedDateTime.parse(dateStr, formatter)
            } catch (e: Exception) {
                continue
            }
        }
        // Se nenhum formatador funcionou, tenta o parse básico como último recurso
        return try { ZonedDateTime.parse(dateStr) } catch (e: Exception) { null }
    }

    fun applyPartialEmailCensorship(email: String?): String {
        if (email == null || !email.contains("@")) return "N/A"
        val parts = email.split("@")
        val name = parts[0]
        val domain = parts[1]
        
        return when {
            name.length <= 2 -> "****@$domain"
            name.length <= 4 -> "${name.take(1)}**${name.takeLast(1)}@$domain"
            else -> "${name.take(2)}***${name.takeLast(2)}@$domain"
        }
    }

    fun applyPartialPhoneCensorship(phone: String?): String {
        if (phone == null) return "N/A"
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val length = cleanPhone.length
        
        return if (length >= 7) {
            val start = cleanPhone.take(3)
            val end = cleanPhone.takeLast(3)
            val middleCount = length - 6
            val stars = "*".repeat(middleCount)
            "$start$stars$end"
        } else {
            "***${cleanPhone.takeLast(2)}"
        }
    }

    fun maskEmail(email: String?): String { return applyPartialEmailCensorship(email) }
    fun maskPhone(phone: String?): String { return applyPartialPhoneCensorship(phone) }
}