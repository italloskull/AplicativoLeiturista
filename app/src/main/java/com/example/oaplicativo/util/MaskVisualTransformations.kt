package com.example.oaplicativo.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Máscara para Data: DD/MM/AAAA
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0, 8) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * Máscara para CPF (000.000.000-00) ou CNPJ (00.000.000/0000-00)
 */
class CpfCnpjVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text.filter { it.isDigit() }
        val isCnpj = input.length > 11
        val trimmed = if (isCnpj) {
            if (input.length >= 14) input.substring(0, 14) else input
        } else {
            if (input.length >= 11) input.substring(0, 11) else input
        }

        var out = ""
        if (isCnpj) {
            // 00.000.000/0000-00
            for (i in trimmed.indices) {
                out += trimmed[i]
                when (i) {
                    1, 4 -> out += "."
                    7 -> out += "/"
                    11 -> out += "-"
                }
            }
        } else {
            // 000.000.000-00
            for (i in trimmed.indices) {
                out += trimmed[i]
                when (i) {
                    2, 5 -> out += "."
                    8 -> out += "-"
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val cleanOffset = if (offset > trimmed.length) trimmed.length else offset
                var transformed = cleanOffset
                if (isCnpj) {
                    if (cleanOffset > 1) transformed++
                    if (cleanOffset > 4) transformed++
                    if (cleanOffset > 7) transformed++
                    if (cleanOffset > 11) transformed++
                } else {
                    if (cleanOffset > 2) transformed++
                    if (cleanOffset > 5) transformed++
                    if (cleanOffset > 8) transformed++
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                var original = offset
                if (isCnpj) {
                    if (offset > 2) original--
                    if (offset > 6) original--
                    if (offset > 10) original--
                    if (offset > 15) original--
                } else {
                    if (offset > 3) original--
                    if (offset > 7) original--
                    if (offset > 10) original--
                }
                return if (original > trimmed.length) trimmed.length else original
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * Máscara para Telefone: (00) 00000-0000 ou (00) 0000-0000
 */
class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text.filter { it.isDigit() }
        val trimmed = if (input.length >= 11) input.substring(0, 11) else input
        
        var out = ""
        for (i in trimmed.indices) {
            if (i == 0) out += "("
            out += trimmed[i]
            if (i == 1) out += ") "
            if (trimmed.length == 11) {
                if (i == 6) out += "-"
            } else {
                if (i == 5) out += "-"
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val cleanOffset = if (offset > trimmed.length) trimmed.length else offset
                var transformed = cleanOffset
                if (cleanOffset > 0) transformed += 1 // (
                if (cleanOffset > 1) transformed += 2 // ) space
                if (trimmed.length == 11) {
                    if (cleanOffset > 6) transformed += 1 // -
                } else {
                    if (cleanOffset > 5) transformed += 1 // -
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                var original = offset
                if (offset > 0) original -= 1
                if (offset > 3) original -= 2
                if (trimmed.length == 11) {
                    if (offset > 9) original -= 1
                } else {
                    if (offset > 8) original -= 1
                }
                return if (original > trimmed.length) trimmed.length else original
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
