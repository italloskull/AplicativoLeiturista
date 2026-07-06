package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.model.UserProfile
import org.json.JSONObject

class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_ECONOMY_UPDATES)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
        db.execSQL(CREATE_TABLE_USER_CACHE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 40) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL("DROP TABLE IF EXISTS grandes_empreendimentos")
            onCreate(db)
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
            put("caixa_padrao", customer.isStandardMeasurementBox)
            put("lacres_padronizados", customer.isStandardizedSeals)
            put("hd_acessivel", customer.isHdAccessible)
            put("veranista", customer.isVacationer)
            put("possui_piscina", customer.possuiPiscina)
            put("possui_caixa_agua", customer.possuiCaixaAgua)
            put("latitude", customer.latitude)
            put("longitude", customer.longitude)
            put("situacao_local", customer.locationStatus)
            put("qtd_economias", customer.economiesCount)
            put("adicionado_por", customer.addedBy)
            put("capturado_em", customer.capturedAt)
            put("date", customer.date)
            put("qualidade", customer.quality)
            put("entrevistado_nome", customer.entrevistadoNome)
            put("entrevistado_cpf", customer.entrevistadoCpf)
            put("entrevistado_mae", customer.entrevistadoMae)
            put("entrevistado_nascimento", customer.entrevistadoNascimento)
            put("entrevistado_sexo", customer.entrevistadoSexo)
            put("entrevistado_apresentou_doc", customer.entrevistadoApresentouDoc)
            put("entrevistado_qual_doc", customer.entrevistadoQualDoc)
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
            put("observacao", customer.observacao)
            put("beneficiario_social", customer.beneficiarioSocial)
            put("usa_agua_vizinho", customer.usaAguaVizinho)
            put("possui_hidrometro", customer.possuiHidrometro)
            put("grupo_sugerido", customer.grupoSugerido)
            put("setor", customer.setor)
            put("quadra", customer.quadra)
            put("local_instalacao", customer.localInstalacao)
            put("acessibilidade", customer.acessibilidade)
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
                // SÊNIOR FIX: Reconstrução COMPLETA do objeto para não perder metadados vitais (Grupo, Rota, etc)
                val customer = Customer(
                    id = currentId,
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                    registrationDigit = cursor.getString(cursor.getColumnIndexOrThrow("digito_matricula")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    celular = cursor.getString(cursor.getColumnIndexOrThrow("celular")),
                    isStandardMeasurementBox = cursor.getString(cursor.getColumnIndexOrThrow("caixa_padrao")),
                    isStandardizedSeals = cursor.getString(cursor.getColumnIndexOrThrow("lacres_padronizados")),
                    isHdAccessible = cursor.getString(cursor.getColumnIndexOrThrow("hd_acessivel")),
                    isVacationer = cursor.getString(cursor.getColumnIndexOrThrow("veranista")),
                    possuiPiscina = cursor.getString(cursor.getColumnIndexOrThrow("possui_piscina")),
                    possuiCaixaAgua = cursor.getString(cursor.getColumnIndexOrThrow("possui_caixa_agua")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    locationStatus = cursor.getString(cursor.getColumnIndexOrThrow("situacao_local")),
                    economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                    capturedAt = cursor.getString(cursor.getColumnIndexOrThrow("capturado_em")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    quality = cursor.getString(cursor.getColumnIndexOrThrow("qualidade")),
                    entrevistadoNome = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nome")),
                    entrevistadoCpf = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_cpf")),
                    entrevistadoMae = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_mae")),
                    entrevistadoNascimento = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nascimento")),
                    entrevistadoSexo = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_sexo")),
                    entrevistadoApresentouDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_apresentou_doc")),
                    entrevistadoQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_qual_doc")),
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
                    observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao")),
                    beneficiarioSocial = cursor.getString(cursor.getColumnIndexOrThrow("beneficiario_social")),
                    usaAguaVizinho = cursor.getString(cursor.getColumnIndexOrThrow("usa_agua_vizinho")),
                    possuiHidrometro = cursor.getString(cursor.getColumnIndexOrThrow("possui_hidrometro")),
                    grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                    setor = cursor.getString(cursor.getColumnIndexOrThrow("setor")),
                    quadra = cursor.getString(cursor.getColumnIndexOrThrow("quadra")),
                    localInstalacao = cursor.getString(cursor.getColumnIndexOrThrow("local_instalacao")),
                    acessibilidade = cursor.getString(cursor.getColumnIndexOrThrow("acessibilidade")),
                    rotaSugerida = cursor.getString(cursor.getColumnIndexOrThrow("rota_sugerida")),
                    numeroHidrometro = cursor.getString(cursor.getColumnIndexOrThrow("numero_hidrometro")),
                    isSynced = false
                )
                list.add(currentId to customer)
            }
        }
        return list
    }

    fun deleteSyncedCustomer(id: String) {
        writableDatabase.delete("customers", "id = ?", arrayOf(id))
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
            put("medidor_energia", item.electricityMeterNumber)
            put("latitude", item.latitude)
            put("longitude", item.longitude)
            put("cidade", item.cidade)
            put("cidade_id", item.cidadeId)
            put("grupo_sugerido", item.grupoSugerido)
            put("rota_sugerida", item.rotaSugerida)
            put("adicionado_por", item.addedBy)
            put("data", item.date)
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
                // SÊNIOR FIX: Reconstrução COMPLETA para Grandes Empreendimentos
                val ge = EconomyUpdate(
                    id = currentId,
                    leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                    hdNumber = cursor.getString(cursor.getColumnIndexOrThrow("numero_hd")),
                    buildingName = cursor.getString(cursor.getColumnIndexOrThrow("nome_edificio")),
                    constructionCompany = cursor.getString(cursor.getColumnIndexOrThrow("construtora")),
                    economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                    floorsCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_pavimentos"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_pavimentos")),
                    electricityMeterNumber = cursor.getString(cursor.getColumnIndexOrThrow("medidor_energia")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("data")),
                    cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                    rotaSugerida = cursor.getString(cursor.getColumnIndexOrThrow("rota_sugerida")),
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

    fun resetSyncAttempts() {
        writableDatabase.execSQL("UPDATE customers SET sync_attempts = 0")
        writableDatabase.execSQL("UPDATE grandes_empreendimentos SET sync_attempts = 0")
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

    fun getTodayStats(city: String? = null, isAdmin: Boolean = false): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM customers WHERE isSynced = 0", null).use { cursor ->
            if (cursor.moveToFirst()) stats["pendentes"] = cursor.getInt(0)
        }
        return stats
    }

    fun getRecadastroStats(city: String? = null, isAdmin: Boolean = false): Pair<Int, Int> {
        val db = readableDatabase
        var total = 0; var synced = 0
        db.rawQuery("SELECT COUNT(*) FROM customers", null).use { cursor -> if (cursor.moveToFirst()) total = cursor.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM customers WHERE isSynced = 1", null).use { cursor -> if (cursor.moveToFirst()) synced = cursor.getInt(0) }
        return total to synced
    }

    fun getEconomyStats(city: String? = null, isAdmin: Boolean = false): Pair<Int, Int> {
        val db = readableDatabase
        var total = 0; var synced = 0
        db.rawQuery("SELECT COUNT(*) FROM grandes_empreendimentos", null).use { cursor -> if (cursor.moveToFirst()) total = cursor.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM grandes_empreendimentos WHERE isSynced = 1", null).use { cursor -> if (cursor.moveToFirst()) synced = cursor.getInt(0) }
        return total to synced
    }

    fun cacheUserProfile(userId: String, email: String, name: String, user: String, isDev: Boolean, cargo: String) {
        val db = writableDatabase
        val obj = JSONObject().apply {
            put("id", userId); put("email", email); put("fullName", name)
            put("username", user); put("cargo", cargo)
        }
        val values = ContentValues().apply {
            put("id", userId)
            put("data", obj.toString())
        }
        db.insertWithOnConflict("user_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCachedUserProfile(userId: String): UserProfile? {
        val db = readableDatabase
        db.query("user_cache", arrayOf("data"), "id = ?", arrayOf(userId), null, null, null).use { cursor ->
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

    companion object {
        @Volatile private var instance: LocalDatabase? = null
        fun getInstance(context: Context): LocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: LocalDatabase(context).also { instance = it }
            }
        }

        private const val DATABASE_NAME = "sanitation_final_v8.db"
        private const val DATABASE_VERSION = 40
        
        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE IF NOT EXISTS customers (
                id TEXT PRIMARY KEY, cidade_id TEXT, leiturista_id TEXT, name TEXT, matricula TEXT, digito_matricula TEXT, email TEXT, celular TEXT,
                caixa_padrao TEXT, lacres_padronizados TEXT, hd_acessivel TEXT, veranista TEXT, possui_piscina TEXT, possui_caixa_agua TEXT,
                latitude REAL, longitude REAL, situacao_local TEXT, qtd_economias INTEGER, adicionado_por TEXT, capturado_em TEXT, date TEXT,
                qualidade TEXT, entrevistado_nome TEXT, entrevistado_cpf TEXT, entrevistado_mae TEXT, entrevistado_nascimento TEXT, entrevistado_sexo TEXT,
                entrevistado_apresentou_doc TEXT, entrevistado_qual_doc TEXT, logradouro TEXT, numero TEXT, complemento TEXT, bairro TEXT, cidade TEXT,
                uf TEXT, cep TEXT, pavimento_rua TEXT, pavimento_calcada TEXT, fonte_abastecimento TEXT, existe_rede_agua TEXT, observacao TEXT,
                beneficiario_social TEXT, usa_agua_vizinho TEXT, possui_hidrometro TEXT, grupo_sugerido TEXT, setor TEXT, quadra TEXT,
                local_instalacao TEXT, acessibilidade TEXT, rota_sugerida TEXT, numero_hidrometro TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT, sincronizado_em TEXT
            )"""
            
        private const val CREATE_TABLE_ECONOMY_UPDATES = """
            CREATE TABLE IF NOT EXISTS grandes_empreendimentos (
                id TEXT PRIMARY KEY, leiturista_id TEXT, numero_hd TEXT, nome_edificio TEXT, construtora TEXT, qtd_economias INTEGER, qtd_pavimentos INTEGER,
                medidor_energia TEXT, latitude REAL, longitude REAL, cidade TEXT, cidade_id TEXT, grupo_sugerido TEXT, rota_sugerida TEXT, adicionado_por TEXT,
                data TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT, sincronizado_em TEXT
            )"""

        private const val CREATE_TABLE_STATS = "CREATE TABLE IF NOT EXISTS stats (id TEXT PRIMARY KEY, value INTEGER)"
        private const val CREATE_TABLE_HISTORY = "CREATE TABLE IF NOT EXISTS history (id TEXT PRIMARY KEY, date TEXT, count INTEGER)"
        private const val CREATE_TABLE_USER_CACHE = "CREATE TABLE IF NOT EXISTS user_cache (id TEXT PRIMARY KEY, data TEXT)"
    }
}
