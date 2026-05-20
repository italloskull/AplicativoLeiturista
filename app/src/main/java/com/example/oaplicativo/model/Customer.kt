package com.example.oaplicativo.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * DATA MODEL: CUSTOMER (100% ALINHADO COM SQL DO SUPABASE)
 * ⚡ OTIMIZAÇÃO: Marcado como @Immutable para economizar bateria e recomposições no Compose.
 */
@Immutable
@Serializable
data class Customer(
    val id: String? = null,
    @SerialName("cidade_id") val cidadeId: String? = null,
    @SerialName("leiturista_id") val leituristaId: String? = null,
    val name: String? = null,
    @SerialName("matricula") val registrationNumber: String? = null,
    @SerialName("digito_matricula") val registrationDigit: String? = null,
    val email: String? = null,
    @SerialName("telefone_fixo") val landline: String? = null,
    val celular: String? = null,
    
    @SerialName("caixa_padrao") val isStandardMeasurementBox: String? = null,
    @SerialName("lacres_padronizados") val isStandardizedSeals: String? = null,
    @SerialName("hd_acessivel") val isHdAccessible: String? = null,
    @SerialName("veranista") val isVacationer: String? = null,
    @SerialName("possui_piscina") val possuiPiscina: String? = null,
    @SerialName("possui_caixa_agua") val possuiCaixaAgua: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("situacao_local") val locationStatus: String? = null,
    @SerialName("qtd_economias") val economiesCount: Int? = null,
    
    @SerialName("criado_em") val createdAt: String? = null,
    @SerialName("adicionado_por") val addedBy: String? = null,
    @SerialName("capturado_em") val capturedAt: String? = null,
    @SerialName("sincronizado_em") val synchronizedAt: String? = null,
    val date: String? = null,
    @SerialName("qualidade") val quality: String? = null,

    // Entrevista
    @SerialName("entrevistado_nome") val entrevistadoNome: String? = null,
    @SerialName("entrevistado_cpf") val entrevistadoCpf: String? = null,
    @SerialName("entrevistado_mae") val entrevistadoMae: String? = null,
    @SerialName("entrevistado_nascimento") val entrevistadoNascimento: String? = null,
    @SerialName("entrevistado_sexo") val entrevistadoSexo: String? = null,
    @SerialName("entrevistado_apresentou_doc") val entrevistadoApresentouDoc: String? = null,
    @SerialName("entrevistado_qual_doc") val entrevistadoQualDoc: String? = null,

    @SerialName("proprietario_nome") val proprietarioNome: String? = null,
    @SerialName("proprietario_cpf") val proprietarioCpf: String? = null,
    @SerialName("proprietario_mae") val proprietarioMae: String? = null,
    @SerialName("proprietario_nascimento") val proprietarioNascimento: String? = null,
    @SerialName("proprietario_sexo") val proprietarioSexo: String? = null,
    @SerialName("proprietario_apresentou_doc") val proprietarioApresentouDoc: String? = null,
    @SerialName("proprietario_qual_doc") val proprietarioQual_doc: String? = null,

    @SerialName("locatario_nome") val locatarioNome: String? = null,
    @SerialName("locatario_cpf") val locatarioCpf: String? = null,
    @SerialName("locatario_mae") val locatarioMae: String? = null,
    @SerialName("locatario_nascimento") val locatarioNascimento: String? = null,
    @SerialName("locatario_sexo") val locatarioSexo: String? = null,
    @SerialName("locatario_apresentou_doc") val locatarioApresentouDoc: String? = null,
    @SerialName("locatario_qual_doc") val locatarioQualDoc: String? = null,

    // Endereço
    val logradouro: String? = null,
    val numero: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val cep: String? = null,
    
    @SerialName("pavimento_rua") val pavimentoRua: String? = null,
    @SerialName("fonte_abastecimento") val fonteAbastecimento: String? = null,
    @SerialName("existe_rede_agua") val existeRedeAgua: String? = null,
    val observacao: String? = null,
    @SerialName("beneficiario_social") val beneficiarioSocial: String? = null,
    @SerialName("usa_agua_vizinho") val usaAguaVizinho: String? = null,
    @SerialName("possui_hidrometro") val possuiHidrometro: String? = null,
    @SerialName("grupo_sugerido") val grupoSugerido: String? = null,
    val setor: String? = null,
    val quadra: String? = null,

    // Compatibilidade Local
    @Transient val localInstalacao: String? = null,
    @Transient val acessibilidade: String? = null,
    @Transient val numeroHidrometro: String? = null,
    @Transient val cellPhone: String? = null,
    @Transient val isSynced: Boolean = true
)
