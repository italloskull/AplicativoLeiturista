package com.example.oaplicativo.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * UTITÁRIO CENTRAL DE DATAS E FUSO HORÁRIO (AMERICA/SAO_PAULO - BRASÍLIA)
 */
object DateUtils {

    val BRASILIA_TIMEZONE: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo")

    /**
     * Retorna a data curta atual no fuso de Brasília no formato 'YYYY/MM/DD'.
     * Exemplo: '2026/09/06'
     */
    fun getTodayBrasilia(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).apply {
            timeZone = BRASILIA_TIMEZONE
        }
        return sdf.format(Calendar.getInstance(BRASILIA_TIMEZONE).time)
    }

    /**
     * Retorna o timestamp completo atual no fuso de Brasília no formato 'YYYY/MM/DD_HH:mm:ss'.
     * Exemplo: '2026/09/06_16:09:38'
     */
    fun getFullTimestampBrasilia(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd_HH:mm:ss", Locale.getDefault()).apply {
            timeZone = BRASILIA_TIMEZONE
        }
        return sdf.format(Calendar.getInstance(BRASILIA_TIMEZONE).time)
    }

    /**
     * Retorna o timestamp para nome de arquivos no fuso de Brasília no formato 'YYYYMMDD_HHMMSS'.
     * Exemplo: '20260906_160938'
     */
    fun getFileNameTimestampBrasilia(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).apply {
            timeZone = BRASILIA_TIMEZONE
        }
        return sdf.format(Calendar.getInstance(BRASILIA_TIMEZONE).time)
    }
}
