package com.example.oaplicativo.util.privacy

object PrivacyUtils {

    /**
     * SÊNIOR MASK: Censura parcial de e-mail.
     * Mostra as 2 primeiras letras, o final do username e o domínio.
     * Ex: "matheuskelvinm@gmail.com" -> "ma**********m@gmail.com"
     */
    fun applyPartialEmailCensorship(email: String?): String {
        if (email.isNullOrBlank()) return ""
        val parts = email.split("@")
        if (parts.size != 2) return email
        
        val username = parts[0]
        val domain = parts[1]
        
        return when {
            username.length <= 2 -> "$username@$domain"
            username.length <= 4 -> "${username.take(1)}**${username.takeLast(1)}@$domain"
            else -> "${username.take(2)}${"*".repeat(username.length - 3)}${username.takeLast(1)}@$domain"
        }
    }

    /**
     * SÊNIOR MASK: Censura parcial de celular.
     * Mantém apenas os 4 últimos dígitos visíveis.
     * Ex: "(19) 46464-9494" -> "(**) *****-9494"
     */
    fun applyPartialPhoneCensorship(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        
        // Remove caracteres não numéricos para processar
        val digits = phone.filter { it.isDigit() }
        
        return if (digits.length >= 4) {
            val lastFour = digits.takeLast(4)
            "(**) *****-$lastFour"
        } else {
            phone 
        }
    }

    /**
     * SÊNIOR MASK: Censura de CPF/CNPJ.
     * Ex: "123.456.789-10" -> "***.***.***-10"
     */
    fun maskCpfCnpj(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val digits = value.filter { it.isDigit() }
        return if (digits.length >= 2) {
            "***.***.***-${digits.takeLast(2)}"
        } else {
            "***.***.***-**"
        }
    }

    /**
     * SÊNIOR POLICY: Define se um registro deve ter censura inicial.
     * Atualmente censuramos se já tiver sido sincronizado ou se for antigo.
     */
    fun shouldMaskSensitiveData(createdAt: String?): Boolean {
        // Se já existe uma data de criação, assumimos que é um registro existente (Edição)
        return !createdAt.isNullOrBlank()
    }
}
