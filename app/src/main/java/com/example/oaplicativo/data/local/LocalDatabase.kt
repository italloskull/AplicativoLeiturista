package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.Cidade
import org.json.JSONObject

class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_ECONOMY_UPDATES)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
        db.execSQL(CREATE_TABLE_USER_CACHE)
        db.execSQL(CREATE_TABLE_CITIES_CACHE)
        db.execSQL(CREATE_TABLE_REMOTE_CACHE)

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_sync ON customers (isSynced)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_city ON customers (cidade)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ge_sync ON grandes_empreendimentos (isSynced)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 46) {
            // SÊNIOR CLEANUP: Migração completa para v46 com resgate de entrevista
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN entrevistado_email TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN entrevistado_celular TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 47) {
            // SÊNIOR CACHE MIGRATION: Garante a criação da tabela de cache remoto
            db.execSQL(CREATE_TABLE_REMOTE_CACHE)
        }
        if (oldVersion < 48) {
            // SÊNIOR DELTA OPTIMIZATION: Adiciona coluna de índice para sincronização incremental
            try {
                db.execSQL("ALTER TABLE remote_cache ADD COLUMN updated_at TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_remote_updated ON remote_cache (updated_at)")
            } catch (_: Exception) {}
        }
        if (oldVersion < 49) {
            // SÊNIOR FIX: Adiciona updated_at na tabela de clientes local para não perder o timestamp no sync
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN updated_at TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 50) {
            // SÊNIOR MIGRATION: Adiciona Lote em clientes e Observações, Blocos, Piscina, Setor, Quadra, Lote em GE
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN lote TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN qtd_blocos INTEGER")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN possui_piscina TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN observacao TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN setor TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN quadra TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN lote TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 51) {
            // SÊNIOR MIGRATION v51: Adiciona campos de assinatura digital no cliente
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN assinatura_url TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN local_assinatura_path TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 52) {
            // SÊNIOR MIGRATION v52: Adiciona campos de assinatura digital em grandes empreendimentos
            try {
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN assinatura_url TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN local_assinatura_path TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 53) {
            // SÊNIOR MIGRATION v53: Adiciona coluna assinatura_hash de 64 caracteres em ambas as tabelas
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN assinatura_hash TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN assinatura_hash TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 54) {
            // SÊNIOR MIGRATION v54: Adiciona coluna assinatura_payload para auditoria direta de Hash
            try {
                db.execSQL("ALTER TABLE customers ADD COLUMN assinatura_payload TEXT")
                db.execSQL("ALTER TABLE grandes_empreendimentos ADD COLUMN assinatura_payload TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 55) {
            // SÊNIOR MIGRATION v55: Migração atômica de hd_acessivel para hidrometro_acessivel_manutencao (API 29+)
            try {
                db.transaction {
                    execSQL("ALTER TABLE customers ADD COLUMN hidrometro_acessivel_manutencao TEXT")
                    execSQL("UPDATE customers SET hidrometro_acessivel_manutencao = hd_acessivel WHERE hd_acessivel IS NOT NULL")
                }
            } catch (_: Exception) {}
        }
    }

    fun saveCustomerOffline(customer: Customer): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", customer.id)
            put("cidade_id", customer.cidadeId)
            put("leiturista_id", customer.leituristaId)
            put("name", customer.name)
            put("matricula", customer.registrationNumber)
            put("digito_matricula", customer.registrationDigit)
            put("email", customer.email)
            put("celular", customer.celular)
            put("caixa_padrao", " ")
            put("lacres_padronizados", customer.isStandardizedSeals)
            put("hidrometro_acessivel_manutencao", customer.isAccessibleForMaintenance)
            put("veranista", customer.isVacationer)
            put("possui_piscina", customer.possuiPiscina)
            put("possui_caixa_agua", customer.possuiCaixaAgua)
            put("latitude", customer.latitude)
            put("longitude", customer.longitude)
            put("situacao_local", customer.locationStatus)
            put("qtd_economias", customer.economiesCount)
            put("adicionado_por", customer.addedBy)
            put("capturado_em", customer.capturedAt)
            put("updated_at", customer.updatedAt)
            put("date", customer.date)
            put("qualidade", customer.quality)
            put("entrevistado_nome", customer.entrevistadoNome)
            put("entrevistado_cpf", customer.entrevistadoCpf)
            put("entrevistado_mae", customer.entrevistadoMae)
            put("entrevistado_nascimento", customer.entrevistadoNascimento)
            put("entrevistado_sexo", customer.entrevistadoSexo)
            put("entrevistado_apresentou_doc", customer.entrevistadoApresentouDoc)
            put("entrevistado_qual_doc", customer.entrevistadoQualDoc)
            put("entrevistado_email", customer.entrevistadoEmail)
            put("entrevistado_celular", customer.entrevistadoCelular)
            put("assinatura_url", customer.assinaturaUrl)
            put("local_assinatura_path", customer.localAssinaturaPath)
            put("assinatura_hash", customer.assinaturaHash)
            put("assinatura_payload", customer.assinaturaPayload)
            put("logradouro", customer.logradouro)
            put("numero", customer.numero)
            put("complemento", customer.complemento)
            put("bairro", customer.bairro)
            put("cidade", customer.cidade)
            put("uf", customer.uf)
            put("cep", customer.cep)
            put("pavimento_rua", customer.pavimentoRua)
            put("pavimento_calcada", customer.pavimentoCalcada)
            put("fonte_abastecimento", customer.fonteAbastecimento)
            put("existe_rede_agua", customer.existeRedeAgua)
            put("local_instalacao", customer.localInstalacao)
            put("acessibilidade_leitura", customer.accessibilityReading)
            put("observacao", customer.observacao)
            put("beneficiario_social", customer.beneficiarioSocial)
            put("usa_agua_vizinho", customer.usaAguaVizinho)
            put("possui_hidrometro", customer.possuiHidrometro)
            put("til_esgoto", customer.tilEsgoto)
            put("medidor_energia", customer.electricityMeter)
            put("grupo_sugerido", customer.grupoSugerido)
            put("setor", customer.setor)
            put("quadra", customer.quadra)
            put("lote", customer.lote)
            put("rota_sugerida", customer.rotaSugerida)
            put("numero_hidrometro", customer.numeroHidrometro)
            put("isSynced", 0)
        }
        return db.insertWithOnConflict("customers", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt()
    }

    fun getPendingCustomers(city: String? = null, isAdmin: Boolean = false): List<Pair<String, Customer>> {
        val list = mutableListOf<Pair<String, Customer>>()
        val db = readableDatabase
        val selection = if (!isAdmin && city != null) "isSynced = 0 AND cidade = ?" else "isSynced = 0"
        val args = if (!isAdmin && city != null) arrayOf(city) else null
        
        db.query("customers", null, selection, args, null, null, "capturado_em DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val currentId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val customer = Customer(
                    id = currentId,
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                    registrationDigit = cursor.getString(cursor.getColumnIndexOrThrow("digito_matricula")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    celular = cursor.getString(cursor.getColumnIndexOrThrow("celular")),
                    isStandardizedSeals = cursor.getString(cursor.getColumnIndexOrThrow("lacres_padronizados")),
                    isAccessibleForMaintenance = cursor.getString(cursor.getColumnIndexOrThrow("hidrometro_acessivel_manutencao")),
                    isVacationer = cursor.getString(cursor.getColumnIndexOrThrow("veranista")),
                    possuiPiscina = cursor.getString(cursor.getColumnIndexOrThrow("possui_piscina")),
                    possuiCaixaAgua = cursor.getString(cursor.getColumnIndexOrThrow("possui_caixa_agua")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    locationStatus = cursor.getString(cursor.getColumnIndexOrThrow("situacao_local")),
                    economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                    capturedAt = cursor.getString(cursor.getColumnIndexOrThrow("capturado_em")),
                    updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    quality = cursor.getString(cursor.getColumnIndexOrThrow("qualidade")),
                    entrevistadoNome = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nome")),
                    entrevistadoCpf = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_cpf")),
                    entrevistadoMae = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_mae")),
                    entrevistadoNascimento = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nascimento")),
                    entrevistadoSexo = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_sexo")),
                    entrevistadoApresentouDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_apresentou_doc")),
                    entrevistadoQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_qual_doc")),
                    entrevistadoEmail = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_email")),
                    entrevistadoCelular = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_celular")),
                    assinaturaUrl = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_url")),
                    localAssinaturaPath = cursor.getString(cursor.getColumnIndexOrThrow("local_assinatura_path")),
                    assinaturaHash = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_hash")),
                    assinaturaPayload = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_payload")),
                    logradouro = cursor.getString(cursor.getColumnIndexOrThrow("logradouro")),
                    numero = cursor.getString(cursor.getColumnIndexOrThrow("numero")),
                    complemento = cursor.getString(cursor.getColumnIndexOrThrow("complemento")),
                    bairro = cursor.getString(cursor.getColumnIndexOrThrow("bairro")),
                    cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                    uf = cursor.getString(cursor.getColumnIndexOrThrow("uf")),
                    cep = cursor.getString(cursor.getColumnIndexOrThrow("cep")),
                    pavimentoRua = cursor.getString(cursor.getColumnIndexOrThrow("pavimento_rua")),
                    pavimentoCalcada = cursor.getString(cursor.getColumnIndexOrThrow("pavimento_calcada")),
                    fonteAbastecimento = cursor.getString(cursor.getColumnIndexOrThrow("fonte_abastecimento")),
                    existeRedeAgua = cursor.getString(cursor.getColumnIndexOrThrow("existe_rede_agua")),
                    localInstalacao = cursor.getString(cursor.getColumnIndexOrThrow("local_instalacao")),
                    accessibilityReading = cursor.getString(cursor.getColumnIndexOrThrow("acessibilidade_leitura")),
                    observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao")),
                    beneficiarioSocial = cursor.getString(cursor.getColumnIndexOrThrow("beneficiario_social")),
                    usaAguaVizinho = cursor.getString(cursor.getColumnIndexOrThrow("usa_agua_vizinho")),
                    possuiHidrometro = cursor.getString(cursor.getColumnIndexOrThrow("possui_hidrometro")),
                    tilEsgoto = cursor.getString(cursor.getColumnIndexOrThrow("til_esgoto")),
                    electricityMeter = cursor.getString(cursor.getColumnIndexOrThrow("medidor_energia")),
                    grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                    setor = cursor.getString(cursor.getColumnIndexOrThrow("setor")),
                    quadra = cursor.getString(cursor.getColumnIndexOrThrow("quadra")),
                    lote = cursor.getString(cursor.getColumnIndexOrThrow("lote")),
                    rotaSugerida = cursor.getString(cursor.getColumnIndexOrThrow("rota_sugerida")),
                    numeroHidrometro = cursor.getString(cursor.getColumnIndexOrThrow("numero_hidrometro")),
                    isSynced = false,
                    lastError = cursor.getString(cursor.getColumnIndexOrThrow("last_error")) // SÊNIOR FIX: Carrega o erro
                )
                list.add(currentId to customer)
            }
        }
        return list
    }

    fun deleteSyncedCustomer(id: String) {
        writableDatabase.delete("customers", "id = ?", arrayOf(id))
    }

    // SÊNIOR DIAGNOSTIC: Grava o motivo da falha de sincronismo para facilitar o suporte
    fun updateCustomerError(id: String, error: String) {
        Log.e("Supauploads", "💾 [SQLITE] Gravando erro para cliente $id: $error")
        val values = ContentValues().apply {
            put("last_error", error)
            put("sync_attempts", 1) // Incrementa sinalizando que houve tentativa
        }
        val rows = writableDatabase.update("customers", values, "id = ?", arrayOf(id))
        Log.d("Supauploads", "💾 [SQLITE] $rows linha(s) atualizada(s) com o erro.")
    }

    fun saveEconomyUpdateOffline(item: EconomyUpdate): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", item.id)
            put("leiturista_id", item.leituristaId)
            put("numero_hd", item.hdNumber)
            put("nome_edificio", item.buildingName)
            put("construtora", item.constructionCompany)
            put("qtd_economias", item.economiesCount)
            put("qtd_pavimentos", item.floorsCount)
            put("qtd_blocos", item.blockCount)
            put("medidor_energia", item.electricityMeterNumber)
            put("til_esgoto", item.tilEsgoto)
            put("acessibilidade_leitura", item.accessibilityReading)
            put("possui_piscina", item.possuiPiscina)
            put("observacao", item.observacao)
            put("latitude", item.latitude)
            put("longitude", item.longitude)
            put("cidade", item.cidade)
            put("cidade_id", item.cidadeId)
            put("grupo_sugerido", item.grupoSugerido)
            put("setor", item.setor)
            put("quadra", item.quadra)
            put("lote", item.lote)
            put("rota_sugerida", item.rotaSugerida)
            put("adicionado_por", item.addedBy)
            put("data", item.date)
            put("assinatura_url", item.assinaturaUrl)
            put("local_assinatura_path", item.localAssinaturaPath)
            put("assinatura_hash", item.assinaturaHash)
            put("assinatura_payload", item.assinaturaPayload)
            put("isSynced", 0)
        }
        return db.insertWithOnConflict("grandes_empreendimentos", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt()
    }

    fun getPendingEconomyUpdates(city: String? = null, isAdmin: Boolean = false): List<Pair<String, EconomyUpdate>> {
        val list = mutableListOf<Pair<String, EconomyUpdate>>()
        val db = readableDatabase
        val selection = if (!isAdmin && city != null) "isSynced = 0 AND cidade = ?" else "isSynced = 0"
        val args = if (!isAdmin && city != null) arrayOf(city) else null

        db.query("grandes_empreendimentos", null, selection, args, null, null, "data DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val currentId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val ge = EconomyUpdate(
                    id = currentId,
                    buildingName = cursor.getString(cursor.getColumnIndexOrThrow("nome_edificio")),
                    cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                    hdNumber = cursor.getString(cursor.getColumnIndexOrThrow("numero_hd")),
                    constructionCompany = cursor.getString(cursor.getColumnIndexOrThrow("construtora")),
                    economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                    floorsCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_pavimentos"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_pavimentos")),
                    blockCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_blocos"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_blocos")),
                    electricityMeterNumber = cursor.getString(cursor.getColumnIndexOrThrow("medidor_energia")),
                    tilEsgoto = cursor.getString(cursor.getColumnIndexOrThrow("til_esgoto")),
                    accessibilityReading = cursor.getString(cursor.getColumnIndexOrThrow("acessibilidade_leitura")),
                    possuiPiscina = cursor.getString(cursor.getColumnIndexOrThrow("possui_piscina")),
                    observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("data")),
                    grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                    setor = cursor.getString(cursor.getColumnIndexOrThrow("setor")),
                    quadra = cursor.getString(cursor.getColumnIndexOrThrow("quadra")),
                    lote = cursor.getString(cursor.getColumnIndexOrThrow("lote")),
                    rotaSugerida = cursor.getString(cursor.getColumnIndexOrThrow("rota_sugerida")),
                    assinaturaUrl = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_url")),
                    localAssinaturaPath = cursor.getString(cursor.getColumnIndexOrThrow("local_assinatura_path")),
                    assinaturaHash = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_hash")),
                    assinaturaPayload = cursor.getString(cursor.getColumnIndexOrThrow("assinatura_payload")),
                    isSynced = false
                )
                list.add(currentId to ge)
            }
        }
        return list
    }

    fun deleteSyncedEconomyUpdate(id: String) {
        writableDatabase.delete("grandes_empreendimentos", "id = ?", arrayOf(id))
    }

    fun getPersonalRecord(): Int {
        val db = readableDatabase
        db.query("stats", arrayOf("value"), "id = 'record'", null, null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 0
    }

    fun updateRecordIfHigher(newVal: Int) {
        val current = getPersonalRecord()
        if (newVal > current) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("id", "record")
                put("value", newVal)
            }
            db.insertWithOnConflict("stats", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getTodayStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM customers WHERE isSynced = 0", null).use { cursor ->
            if (cursor.moveToFirst()) stats["pendentes"] = cursor.getInt(0)
        }
        return stats
    }

    fun cacheUserProfile(userId: String, email: String, name: String, user: String, cargo: String) {
        val db = writableDatabase
        val obj = JSONObject().apply {
            put("id", userId); put("email", email); put("fullName", name)
            put("username", user); put("cargo", cargo)
        }
        val values = ContentValues().apply {
            put("id", user.lowercase().trim()) 
            put("data", obj.toString())
        }
        db.insertWithOnConflict("user_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCachedUserProfile(identifier: String): UserProfile? {
        val db = readableDatabase
        // SÊNIOR FIX: Busca por ID (username) para garantir paridade com o input do login
        db.query("user_cache", arrayOf("data"), "id = ?", arrayOf(identifier.lowercase().trim()), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val data = cursor.getString(0)
                val obj = JSONObject(data)
                return UserProfile(
                    id = obj.getString("id"),
                    email = obj.getString("email"),
                    fullName = obj.optString("fullName"),
                    username = obj.optString("username"),
                    cargo = obj.optString("cargo", "usuário"),
                    cidadeId = null
                )
            }
        }
        return null
    }

    fun cacheUserCities(cities: List<Cidade>) {
        writableDatabase.transaction {
            delete("cities_cache", null, null) 
            cities.forEach { city ->
                val values = ContentValues().apply {
                    put("id", city.id)
                    put("nome", city.nome)
                }
                insert("cities_cache", null, values)
            }
        }
    }

    fun getCachedCities(): List<Cidade> {
        val list = mutableListOf<Cidade>()
        val db = readableDatabase
        db.query("cities_cache", null, null, null, null, null, "nome ASC").use { cursor ->
            while (cursor.moveToNext()) {
                list.add(Cidade(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"))
                ))
            }
        }
        return list
    }

    fun clearCitiesCache() {
        writableDatabase.delete("cities_cache", null, null)
        Log.d("debugs", "🧼 [SQLITE] Cache de cidades limpo com sucesso.")
    }

    // SÊNIOR CLEANUP: Limpeza manual do cache remoto (Útil para testes ou correções de integridade)
    fun clearRemoteCache(city: String? = null) {
        val rows = if (city != null) {
            writableDatabase.delete("remote_cache", "cidade = ?", arrayOf(city))
        } else {
            writableDatabase.delete("remote_cache", null, null)
        }
        Log.d("Supauploads", "🧼 [SQLITE] Cache remoto limpo. Linhas removidas: $rows")
    }

    // SÊNIOR CACHE: Sistema de Cache Remoto com expiração diária
    fun saveRemoteCustomersToCache(customers: List<Customer>) {
        val db = writableDatabase
        val today = com.example.oaplicativo.util.DateUtils.getTodayBrasilia()
        
        db.transaction {
            customers.forEach { customer ->
                val values = ContentValues().apply {
                    put("id", customer.id)
                    put("cidade", customer.cidade)
                    put("updated_at", customer.updatedAt ?: customer.capturedAt) // SÊNIOR INDEX: Para Delta Sync rápido
                    put("data_json", JSONObject().apply {
                        put("id", customer.id)
                        put("cidade_id", customer.cidadeId)
                        put("leiturista_id", customer.leituristaId)
                        put("name", customer.name)
                        put("matricula", customer.registrationNumber)
                        put("numero_hidrometro", customer.numeroHidrometro)
                        put("latitude", customer.latitude)
                        put("longitude", customer.longitude)
                        put("qualidade", customer.quality)
                        put("capturado_em", customer.capturedAt)
                        put("updated_at", customer.updatedAt) // Armazena o tempo da última edição
                        put("isSynced", true)
                    }.toString())
                    put("cached_at", today)
                }
                insertWithOnConflict("remote_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    fun getRemoteCache(city: String? = null): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = readableDatabase
        val today = com.example.oaplicativo.util.DateUtils.getTodayBrasilia()
        
        // SÊNIOR CLEANUP: Limpa cache antigo antes de ler (Reset Diário)
        db.delete("remote_cache", "cached_at != ?", arrayOf(today))

        val selection = if (city != null) "cidade = ?" else null
        val args = if (city != null) arrayOf(city) else null

        db.query("remote_cache", arrayOf("data_json"), selection, args, null, null, "updated_at DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val json = JSONObject(cursor.getString(0))
                list.add(Customer(
                    id = json.optString("id"),
                    cidadeId = json.optString("cidade_id"),
                    leituristaId = json.optString("leiturista_id"),
                    name = json.optString("name"),
                    registrationNumber = json.optString("matricula"),
                    numeroHidrometro = json.optString("numero_hidrometro"),
                    latitude = if (json.isNull("latitude")) null else json.getDouble("latitude"),
                    longitude = if (json.isNull("longitude")) null else json.getDouble("longitude"),
                    quality = json.optString("qualidade"),
                    capturedAt = json.optString("capturado_em"),
                    updatedAt = json.optString("updated_at"), // SÊNIOR DELTA: Recupera o tempo da edição
                    isSynced = true
                ))
            }
        }
        return list
    }

    // SÊNIOR DELTA HELPER: Retorna o timestamp da atualização mais recente no cache via índice
    fun getLatestCacheTimestamp(city: String?): String? {
        val db = readableDatabase
        val selection = if (city != null) "cidade = ?" else null
        val args = if (city != null) arrayOf(city) else null
        
        // SÊNIOR PERF: Busca direta na coluna de índice (Muito mais rápido que processar JSON)
        db.query("remote_cache", arrayOf("updated_at"), selection, args, null, null, "updated_at DESC", "1").use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0).ifBlank { null }
            }
        }
        return null
    }

    companion object {
        @Volatile private var instance: LocalDatabase? = null
        fun getInstance(context: Context): LocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: LocalDatabase(context).also { instance = it }
            }
        }

        private const val DATABASE_NAME = "sanitation_final_v17.db"
        private const val DATABASE_VERSION = 55
        
        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE IF NOT EXISTS customers (
                id TEXT PRIMARY KEY, cidade_id TEXT, leiturista_id TEXT, name TEXT, matricula TEXT, digito_matricula TEXT, email TEXT, celular TEXT,
                caixa_padrao TEXT, lacres_padronizados TEXT, hd_acessivel TEXT, hidrometro_acessivel_manutencao TEXT, veranista TEXT, possui_piscina TEXT, possui_caixa_agua TEXT,
                latitude REAL, longitude REAL, situacao_local TEXT, qtd_economias INTEGER, adicionado_por TEXT, capturado_em TEXT, updated_at TEXT, date TEXT,
                qualidade TEXT, entrevistado_nome TEXT, entrevistado_cpf TEXT, entrevistado_mae TEXT, entrevistado_nascimento TEXT, entrevistado_sexo TEXT,
                entrevistado_apresentou_doc TEXT, entrevistado_qual_doc TEXT, logradouro TEXT, numero TEXT, complemento TEXT, bairro TEXT, cidade TEXT,
                uf TEXT, cep TEXT, pavimento_rua TEXT, pavimento_calcada TEXT, fonte_abastecimento TEXT, existe_rede_agua TEXT, local_instalacao TEXT,
                acessibilidade_leitura TEXT, observacao TEXT, beneficiario_social TEXT, usa_agua_vizinho TEXT, possui_hidrometro TEXT, til_esgoto TEXT, medidor_energia TEXT,
                grupo_sugerido TEXT, setor TEXT, quadra TEXT, lote TEXT, rota_sugerida TEXT, numero_hidrometro TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT, sincronizado_em TEXT,
                entrevistado_email TEXT, entrevistado_celular TEXT, assinatura_url TEXT, local_assinatura_path TEXT, assinatura_hash TEXT, assinatura_payload TEXT
            )"""
            
        private const val CREATE_TABLE_ECONOMY_UPDATES = """
            CREATE TABLE IF NOT EXISTS grandes_empreendimentos (
                id TEXT PRIMARY KEY, leiturista_id TEXT, numero_hd TEXT, nome_edificio TEXT, construtora TEXT, qtd_economias INTEGER, qtd_pavimentos INTEGER, qtd_blocos INTEGER,
                medidor_energia TEXT, til_esgoto TEXT, acessibilidade_leitura TEXT, possui_piscina TEXT, observacao TEXT, latitude REAL, longitude REAL, cidade TEXT, cidade_id TEXT, grupo_sugerido TEXT, setor TEXT, quadra TEXT, lote TEXT, rota_sugerida TEXT, adicionado_por TEXT,
                data TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT, sincronizado_em TEXT, assinatura_url TEXT, local_assinatura_path TEXT, assinatura_hash TEXT, assinatura_payload TEXT
            )"""

        private const val CREATE_TABLE_STATS = "CREATE TABLE IF NOT EXISTS stats (id TEXT PRIMARY KEY, value INTEGER)"
        private const val CREATE_TABLE_HISTORY = "CREATE TABLE IF NOT EXISTS history (id TEXT PRIMARY KEY, date TEXT, count INTEGER)"
        private const val CREATE_TABLE_USER_CACHE = "CREATE TABLE IF NOT EXISTS user_cache (id TEXT PRIMARY KEY, data TEXT)"
        private const val CREATE_TABLE_CITIES_CACHE = "CREATE TABLE IF NOT EXISTS cities_cache (id TEXT PRIMARY KEY, nome TEXT)"
        private const val CREATE_TABLE_REMOTE_CACHE = """
            CREATE TABLE IF NOT EXISTS remote_cache (
                id TEXT PRIMARY KEY, 
                cidade TEXT, 
                updated_at TEXT,
                data_json TEXT, 
                cached_at TEXT
            )"""
    }
}
