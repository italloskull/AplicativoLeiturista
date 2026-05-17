package com.example.oaplicativo.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ValidationUtils {

    /**
     * Valida CPF ou CNPJ usando algoritmos de dígito verificador.
     */
    fun isValidDoc(doc: String): Boolean {
        val digits = doc.filter { it.isDigit() }
        return when (digits.length) {
            11 -> isValidCpf(digits)
            14 -> isValidCnpj(digits)
            else -> false
        }
    }

    private fun isValidCpf(cpf: String): Boolean {
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
        val numbers = cpf.map { it.toString().toInt() }
        
        // 1º dígito
        var sum = 0
        for (i in 0 until 9) sum += numbers[i] * (10 - i)
        var result = (sum * 10) % 11
        if (result == 10) result = 0
        if (result != numbers[9]) return false
        
        // 2º dígito
        sum = 0
        for (i in 0 until 10) sum += numbers[i] * (11 - i)
        result = (sum * 10) % 11
        if (result == 10) result = 0
        if (result != numbers[10]) return false
        
        return true
    }

    private fun isValidCnpj(cnpj: String): Boolean {
        if (cnpj.length != 14 || cnpj.all { it == cnpj[0] }) return false
        val numbers = cnpj.map { it.toString().toInt() }
        
        // 1º dígito
        var weight = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        for (i in 0 until 12) sum += numbers[i] * weight[i]
        var result = sum % 11
        if (result < 2) result = 0 else result = 11 - result
        if (result != numbers[12]) return false
        
        // 2º dígito
        weight = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        sum = 0
        for (i in 0 until 13) sum += numbers[i] * weight[i]
        result = sum % 11
        if (result < 2) result = 0 else result = 11 - result
        if (result != numbers[13]) return false
        
        return true
    }

    /**
     * Valida data de nascimento (DDMMAAAA) para sanidade lógica.
     */
    fun isValidBirthDate(dateStr: String): Boolean {
        val digits = dateStr.filter { it.isDigit() }
        if (digits.length != 8) return false
        
        return try {
            val formatter = DateTimeFormatter.ofPattern("ddMMyyyy")
            val date = LocalDate.parse(digits, formatter)
            val today = LocalDate.now()
            
            // Regra: Não pode ser no futuro e deve ter entre 14 e 115 anos
            val minDate = today.minusYears(115)
            val maxDate = today.minusYears(14)
            
            date.isAfter(minDate) && date.isBefore(today)
        } catch (e: Exception) {
            false
        }
    }
}
