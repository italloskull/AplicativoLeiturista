package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Customer(
    val id: String? = null,
    @SerialName("cidade_id") val cidadeId: String? = null,
    val name: String? = null,
    @SerialName("matricula") val registrationNumber: String? = null,
    @SerialName("digito_matricula") val registrationDigit: String? = null,
    val email: String? = null,
    @SerialName("telefone_fixo") val landline: String? = null,
    @SerialName("celular") val cellPhone: String? = null,
    
    @SerialName("caixa_padrao") val isStandardMeasurementBox: Boolean? = null,
    @SerialName("lacres_padronizados") val isStandardizedSeals: Boolean? = null,
    @SerialName("hd_acessivel") val isHdAccessible: Boolean? = null,
    @SerialName("veranista") val isVacationer: Boolean? = null,
    
    @SerialName("possui_piscina") val possuiPiscina: Boolean? = null,
    @SerialName("possui_caixa_agua") val possuiCaixaAgua: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("situacao_local") val locationStatus: String? = null,
    @SerialName("qtd_economias") val economiesCount: Int? = null,
    
    @SerialName("criado_em") val createdAt: String? = null,
    @SerialName("adicionado_por") val addedBy: String? = null,
    @SerialName("capturado_em") val capturedAt: String? = null,
    @SerialName("sincronizado_em") val syncedAt: String? = null,
    val date: String? = null,

    @SerialName("qualidade") val quality: String? = null,

    // Campos Multi-Papel traduzidos
    @SerialName("entrevistado_nome") val entrevistadoNome: String? = null,
    @SerialName("entrevistado_cpf") val entrevistadoCpf: String? = null,
    @SerialName("entrevistado_mae") val entrevistadoMae: String? = null,
    @SerialName("entrevistado_nascimento") val entrevistadoNascimento: String? = null,
    @SerialName("entrevistado_sexo") val entrevistadoSexo: String? = null,
    @SerialName("entrevistado_apresentou_doc") val entrevistadoApresentouDoc: Boolean? = null,
    @SerialName("entrevistado_qual_doc") val entrevistadoQualDoc: String? = null,

    @SerialName("proprietario_nome") val proprietarioNome: String? = null,
    @SerialName("proprietario_cpf") val proprietarioCpf: String? = null,
    @SerialName("proprietario_mae") val proprietarioMae: String? = null,
    @SerialName("proprietario_nascimento") val proprietarioNascimento: String? = null,
    @SerialName("proprietario_sexo") val proprietarioSexo: String? = null,
    @SerialName("proprietario_apresentou_doc") val proprietarioApresentouDoc: Boolean? = null,
    @SerialName("proprietario_qual_doc") val proprietarioQual_doc: String? = null,

    @SerialName("locatario_nome") val locatarioNome: String? = null,
    @SerialName("locatario_cpf") val locatarioCpf: String? = null,
    @SerialName("locatario_mae") val locatarioMae: String? = null,
    @SerialName("locatario_nascimento") val locatarioNascimento: String? = null,
    @SerialName("locatario_sexo") val locatarioSexo: String? = null,
    @SerialName("locatario_apresentou_doc") val locatarioApresentouDoc: Boolean? = null,
    @SerialName("locatario_qual_doc") val locatarioQualDoc: String? = null,

    // Campos de Endereço traduzidos
    val logradouro: String? = null,
    val numero: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val cep: String? = null,
    
    @SerialName("pavimento_rua") val pavimentoRua: String? = null,
    @SerialName("pavimento_calcada") val pavimentoCalcada: String? = null,
    @SerialName("fonte_abastecimento") val fonteAbastecimento: String? = null,
    @SerialName("existe_rede_agua") val existeRedeAgua: Boolean? = null,
    
    val observacao: String? = null,

    @Transient val isSynced: Boolean = true
)
