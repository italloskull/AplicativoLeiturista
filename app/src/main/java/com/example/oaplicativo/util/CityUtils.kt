package com.example.oaplicativo.util

object CityUtils {
    /**
     * SÊNIOR HELPER: Mapeia IDs fixos de cidades para seus nomes humanos.
     * Centralizado para evitar divergências em filtros regionais (BI e Busca).
     */
    fun getFriendlyCityName(cidadeId: String?): String? {
        return when (cidadeId) {
            "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
            "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
            "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
            "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
            "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
            else -> null
        }
    }
}
