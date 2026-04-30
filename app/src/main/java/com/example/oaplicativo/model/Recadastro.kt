package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recadastro(
    val id: String? = null,
    val matricula: String,
    val lote: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Dados Pessoais
    @SerialName("nome_completo") val nomeCompleto: String,
    @SerialName("cpf_cnpj") val cpfCnpj: String,
    @SerialName("nome_mae") val nomeMae: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null,
    val sexo: String? = null,
    @SerialName("apresentou_documento") val apresentouDocumento: Boolean = false,
    @SerialName("qual_documento") val qualDocumento: String? = null,
    // Vínculo
    @SerialName("is_proprietario") val isProprietario: Boolean = false,
    @SerialName("is_morador") val isMorador: Boolean = false,
    // Contato
    val email: String? = null,
    val telefone: String? = null,
    val celular1: String? = null,
    val celular2: String? = null,
    val celular3: String? = null,
    val celular4: String? = null,
    // Endereço
    val logradouro: String,
    val numero: String,
    val complemento: String? = null,
    val bairro: String,
    val cep: String,
    // Características
    @SerialName("numero_moradores") val numeroMoradores: Int? = null,
    @SerialName("pavimento_rua") val pavimentoRua: String? = null,
    @SerialName("pavimento_calcada") val pavimentoCalcada: String? = null,
    @SerialName("fonte_abastecimento") val fonteAbastecimento: String? = null,
    @SerialName("categoria_1") val categoria1: String? = null,
    @SerialName("categoria_2") val categoria2: String? = null,
    @SerialName("situacao_imovel") val situacaoImovel: String? = null,
    @SerialName("situacao_agua") val situacaoAgua: String? = null,
    // Hidrometria
    @SerialName("possui_hidrometro") val possuiHidrometro: Boolean = false,
    @SerialName("numero_hidrometro") val numeroHidrometro: String? = null,
    @SerialName("local_instalacao") val localInstalacao: String? = null,
    val acessibilidade: String? = null,
    val economias: Int? = null,
    @SerialName("qualidade_cadastrado") val qualidadeCadastrado: String? = null,
    // Metadados
    val observacao: String? = null,
    val fotos: List<String> = emptyList(),
    @SerialName("nome_colaborador") val nomeColaborador: String? = null,
    @SerialName("informant_name") val informantName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)