package com.example.oaplicativo.data.remote.viacep

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepService {
    @GET("{cep}/json/")
    suspend fun getAddressByCep(@Path("cep") cep: String): Response<CepResponse>
}
