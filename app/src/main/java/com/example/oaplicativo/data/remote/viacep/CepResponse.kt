package com.example.oaplicativo.data.remote.viacep

import com.google.gson.annotations.SerializedName

data class CepResponse(
    val cep: String,
    val logradouro: String,
    val complemento: String,
    val bairro: String,
    val localidade: String,
    val uf: String,
    val ibge: String,
    val gia: String,
    val ddd: String,
    val siafi: String,
    @SerializedName("erro") val erro: Boolean? = false
)
