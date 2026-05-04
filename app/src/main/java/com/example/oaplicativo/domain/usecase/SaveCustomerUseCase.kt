package com.example.oaplicativo.domain.usecase

import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Use Case: Salvar Cliente
 * Encapsula a lógica de negócio de preenchimento de metadados e persistência.
 */
class SaveCustomerUseCase(private val repository: CustomerRepository) {
    
    suspend operator fun invoke(customer: Customer, quality: String, userFullName: String) {
        val fullNow = ZonedDateTime.now()
        
        val finalCustomer = customer.copy(
            addedBy = userFullName,
            quality = quality,
            createdAt = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            date = fullNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
            isSynced = false // Inicia como não sincronizado para o Worker processar
        )

        repository.addCustomer(finalCustomer)
    }
}
