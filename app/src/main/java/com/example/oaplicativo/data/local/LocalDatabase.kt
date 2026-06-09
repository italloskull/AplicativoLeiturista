package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.model.UserProfile

class LocalDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // SÊNIOR PERF: Mutex global para impedir Database Lock entre SyncWorker e UI
    private val mutex = kotlinx.coroutines.sync.Mutex()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
        db.execSQL(CREATE_TABLE_USER_CACHE)
        db.execSQL(CREATE_TABLE_ECONOMY_UPDATES)
        // Aplicando índices de alta performance
        db.execSQL(IDX_CUSTOMERS_SYNC)
        db.execSQL(IDX_ECONOMY_SYNC)
        db.execSQL(IDX_CUSTOMERS_CITY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("LocalDatabase", "Migrando banco de $oldVersion para $newVersion")
        
        if (oldVersion < 27) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL(CREATE_TABLE_CUSTOMERS)
        }

        if (oldVersion < 22) {
            try { db.execSQL(CREATE_TABLE_USER_CACHE) } catch (_: Exception) {}
        }
    }

    fun purgeOldRecords() {
        writableDatabase.delete("customers", "isSynced = 1", null)
    }

    fun updateRecordIfHigher(currentCount: Int) {
        val record = getPersonalRecord()
        if (currentCount > record) {
            val values = ContentValues().apply {
                put("record_value", currentCount)
                put("date_achieved", System.currentTimeMillis().toString())
            }
            writableDatabase.insertWithOnConflict("stats", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getPersonalRecord(): Int {
        val cursor = readableDatabase.rawQuery("SELECT MAX(record_value) FROM stats", null)
        var record = 0
        if (cursor.moveToFirst()) record = cursor.getInt(0)
        cursor.close()
        return record
    }

    fun getTodayStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        try {
            // Busca estatísticas reais baseadas no campo 'date' (formato yyyy/MM/dd)
            val todayDate = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            
            val query = "SELECT qualidade, COUNT(*) FROM customers WHERE date = ? GROUP BY qualidade"
            val cursor = readableDatabase.rawQuery(query, arrayOf(todayDate))
            
            var total = 0
            while (cursor.moveToNext()) {
                val qualidade = cursor.getString(0) ?: "Indefinida"
                val count = cursor.getInt(1)
                stats[qualidade] = count
                total += count
            }
            stats["Total"] = total
            cursor.close()
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Erro ao buscar stats de hoje: ${e.message}")
        }
        return stats
    }

    fun getRecadastroStats(): Pair<Int, Int> {
        var total = 0
        var pending = 0
        try {
            val cursorTotal = readableDatabase.rawQuery("SELECT COUNT(*) FROM customers", null)
            if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
            cursorTotal.close()
            // SÊNIOR FIX: Ignoramos registros com falha crítica no contador
            val cursorPending = readableDatabase.rawQuery("SELECT COUNT(*) FROM customers WHERE isSynced = 0 AND sync_attempts < 5", null)
            if (cursorPending.moveToFirst()) pending = cursorPending.getInt(0)
            cursorPending.close()
        } catch (_: Exception) {}
        return Pair(total, pending)
    }

    fun getEconomyStats(): Pair<Int, Int> {
        var total = 0
        var pending = 0
        try {
            // SÊNIOR DEBUG FIX: Garantindo que a consulta ignore o limite de 5 se estivermos forçando
            val cursorTotal = readableDatabase.rawQuery("SELECT COUNT(*) FROM economy_updates", null)
            if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
            cursorTotal.close()
            
            // Verificamos o que o SQLite REALMENTE considera pendente (isSynced = 0)
            val cursorPending = readableDatabase.rawQuery("SELECT COUNT(*) FROM economy_updates WHERE isSynced = 0", null)
            if (cursorPending.moveToFirst()) pending = cursorPending.getInt(0)
            cursorPending.close()
            
            Log.d("LocalDB", "📊 Stats Economia: Total=$total, Pendentes=$pending")
        } catch (_: Exception) {}
        return Pair(total, pending)
    }

    fun saveCustomerOffline(customer: Customer) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("id", customer.id)
                put("name", customer.name)
                put("matricula", customer.registrationNumber)
                put("digito_matricula", customer.registrationDigit)
                put("email", customer.email)
                put("setor", customer.setor)
                put("quadra", customer.quadra)
                put("celular", customer.celular)
                put("telefone_fixo", customer.landline)
                
                put("caixa_padrao", customer.isStandardMeasurementBox)
                put("lacres_padronizados", customer.isStandardizedSeals)
                put("hd_acessivel", customer.isHdAccessible)
                put("veranista", customer.isVacationer)
                put("possui_piscina", customer.possuiPiscina)
                put("beneficiario_social", customer.beneficiarioSocial)
                put("usa_agua_vizinho", customer.usaAguaVizinho)
                put("possui_hidrometro", customer.possuiHidrometro)
                put("existe_rede_agua", customer.existeRedeAgua)

                put("possui_caixa_agua", customer.possuiCaixaAgua)
                put("latitude", customer.latitude)
                put("longitude", customer.longitude)
                put("situacao_local", customer.locationStatus)
                put("qtd_economias", customer.economiesCount)
                put("criado_em", customer.createdAt)
                put("capturado_em", customer.capturedAt)
                put("adicionado_por", customer.addedBy)
                put("cidade_id", customer.cidadeId)
                put("leiturista_id", customer.leituristaId)
                put("date", customer.date)
                put("qualidade", customer.quality)
                put("entrevistado_nome", customer.entrevistadoNome)
                put("entrevistado_cpf", customer.entrevistadoCpf)
                put("entrevistado_mae", customer.entrevistadoMae)
                put("entrevistado_nascimento", customer.entrevistadoNascimento)
                put("entrevistado_sexo", customer.entrevistadoSexo)
                put("entrevistado_apresentou_doc", customer.entrevistadoApresentouDoc)
                put("entrevistado_qual_doc", customer.entrevistadoQualDoc)
                
                put("proprietario_nome", customer.proprietarioNome)
                put("proprietario_cpf", customer.proprietarioCpf)
                put("proprietario_mae", customer.proprietarioMae)
                put("proprietario_nascimento", customer.proprietarioNascimento)
                put("proprietario_sexo", customer.proprietarioSexo)
                put("proprietario_apresentou_doc", customer.proprietarioApresentouDoc)
                put("proprietario_qual_doc", customer.proprietarioQual_doc)
                
                put("locatario_nome", customer.locatarioNome)
                put("locatario_cpf", customer.locatarioCpf)
                put("locatario_mae", customer.locatarioMae)
                put("locatario_nascimento", customer.locatarioNascimento)
                put("locatario_sexo", customer.locatarioSexo)
                put("locatario_apresentou_doc", customer.locatarioApresentouDoc)
                put("locatario_qual_doc", customer.locatarioQualDoc)
                
                put("logradouro", customer.logradouro)
                put("numero", customer.numero)
                put("complemento", customer.complemento)
                put("bairro", customer.bairro)
                put("uf", customer.uf)
                put("cep", customer.cep)
                put("cidade", customer.cidade)
                put("pavimento_rua", customer.pavimentoRua)
                put("pavimento_calcada", customer.pavimentoCalcada)
                put("fonte_abastecimento", customer.fonteAbastecimento)
                put("local_instalacao", customer.localInstalacao)
                put("acessibilidade", customer.acessibilidade)
                put("observacao", customer.observacao)
                put("grupo_sugerido", customer.grupoSugerido)
                put("rota_sugerida", customer.rotaSugerida)
                put("isSynced", 0)
            }
            db.insertWithOnConflict("customers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingCustomers(): List<Pair<String, Customer>> {
        val list = mutableListOf<Pair<String, Customer>>()
        // SÊNIOR FIX: Limitamos a 5 tentativas de sincronização para evitar 'Poison Pill' drenando bateria
        val cursor = readableDatabase.query("customers", null, "isSynced = 0 AND sync_attempts < 5", null, null, null, "criado_em ASC")
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            
            fun getStrOrNull(column: String): String? {
                val idx = cursor.getColumnIndexOrThrow(column)
                return if (cursor.isNull(idx)) null else cursor.getString(idx)
            }

            val customer = Customer(
                id = id,
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                registrationDigit = cursor.getString(cursor.getColumnIndexOrThrow("digito_matricula")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                setor = cursor.getString(cursor.getColumnIndexOrThrow("setor")),
                quadra = cursor.getString(cursor.getColumnIndexOrThrow("quadra")),
                celular = cursor.getString(cursor.getColumnIndexOrThrow("celular")),
                landline = cursor.getString(cursor.getColumnIndexOrThrow("telefone_fixo")),
                
                isStandardMeasurementBox = getStrOrNull("caixa_padrao"),
                isStandardizedSeals = getStrOrNull("lacres_padronizados"),
                isHdAccessible = getStrOrNull("hd_acessivel"),
                isVacationer = getStrOrNull("veranista"),
                possuiPiscina = getStrOrNull("possui_piscina"),
                beneficiarioSocial = getStrOrNull("beneficiario_social"),
                usaAguaVizinho = getStrOrNull("usa_agua_vizinho"),
                possuiHidrometro = getStrOrNull("possui_hidrometro"),
                existeRedeAgua = getStrOrNull("existe_rede_agua"),

                possuiCaixaAgua = cursor.getString(cursor.getColumnIndexOrThrow("possui_caixa_agua")),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                locationStatus = cursor.getString(cursor.getColumnIndexOrThrow("situacao_local")),
                economiesCount = cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("criado_em")),
                capturedAt = cursor.getString(cursor.getColumnIndexOrThrow("capturado_em")),
                addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                quality = cursor.getString(cursor.getColumnIndexOrThrow("qualidade")),
                entrevistadoNome = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nome")),
                entrevistadoCpf = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_cpf")),
                entrevistadoMae = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_mae")),
                entrevistadoNascimento = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nascimento")),
                entrevistadoSexo = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_sexo")),
                entrevistadoApresentouDoc = getStrOrNull("entrevistado_apresentou_doc"),
                entrevistadoQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_qual_doc")),
                proprietarioNome = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_nome")),
                proprietarioCpf = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_cpf")),
                proprietarioMae = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_mae")),
                proprietarioNascimento = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_nascimento")),
                proprietarioSexo = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_sexo")),
                proprietarioApresentouDoc = getStrOrNull("proprietario_apresentou_doc"),
                proprietarioQual_doc = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_qual_doc")),
                locatarioNome = cursor.getString(cursor.getColumnIndexOrThrow("locatario_nome")),
                locatarioCpf = cursor.getString(cursor.getColumnIndexOrThrow("locatario_cpf")),
                locatarioMae = cursor.getString(cursor.getColumnIndexOrThrow("locatario_mae")),
                locatarioNascimento = cursor.getString(cursor.getColumnIndexOrThrow("locatario_nascimento")),
                locatarioSexo = cursor.getString(cursor.getColumnIndexOrThrow("locatario_sexo")),
                locatarioApresentouDoc = getStrOrNull("locatario_apresentou_doc"),
                locatarioQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("locatario_qual_doc")),
                logradouro = cursor.getString(cursor.getColumnIndexOrThrow("logradouro")),
                numero = cursor.getString(cursor.getColumnIndexOrThrow("numero")),
                complemento = cursor.getString(cursor.getColumnIndexOrThrow("complemento")),
                bairro = cursor.getString(cursor.getColumnIndexOrThrow("bairro")),
                uf = cursor.getString(cursor.getColumnIndexOrThrow("uf")),
                cep = cursor.getString(cursor.getColumnIndexOrThrow("cep")),
                cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                pavimentoRua = getStrOrNull("pavimento_rua"),
                pavimentoCalcada = getStrOrNull("pavimento_calcada"),
                fonteAbastecimento = getStrOrNull("fonte_abastecimento"),
                localInstalacao = getStrOrNull("local_instalacao"),
                acessibilidade = getStrOrNull("acessibilidade"),
                observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao")),
                grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                rotaSugerida = getStrOrNull("rota_sugerida"),
                isSynced = false
            )
            list.add(Pair(id, customer))
        }
        cursor.close()
        return list
    }

    fun deleteSyncedCustomer(localId: String) {
        val db = writableDatabase
        db.delete("customers", "id = ?", arrayOf(localId))
    }

    fun saveEconomyUpdateOffline(item: EconomyUpdate) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = android.content.ContentValues().apply {
                put("id", item.id ?: java.util.UUID.randomUUID().toString())
                put("cidade_id", item.cidadeId)
                put("leiturista_id", item.leituristaId)
                put("hdNumber", item.hdNumber)
                put("buildingName", item.buildingName)
                put("constructionCompany", item.constructionCompany)
                put("economiesCount", item.economiesCount)
                put("floorsCount", item.floorsCount)
                put("electricityMeterNumber", item.electricityMeterNumber)
                put("latitude", item.latitude)
                put("longitude", item.longitude)
                put("addedBy", item.addedBy)
                put("createdAt", item.createdAt)
                put("date", item.date)
                put("isSynced", 0)
            }
            db.insertWithOnConflict("economy_updates", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
            Log.d("LocalDB", "✅ Economia salva no SQLite com ID: ${item.id}")
        } catch (e: Exception) {
            Log.e("LocalDB", "❌ ERRO AO SALVAR ECONOMIA", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingEconomyUpdates(): List<Pair<String, EconomyUpdate>> {
        val list = mutableListOf<Pair<String, EconomyUpdate>>()
        val db = readableDatabase
        // SÊNIOR FIX: Limitamos a 5 tentativas de sincronização para evitar 'Poison Pill' drenando bateria
        val cursor = db.rawQuery("SELECT * FROM economy_updates WHERE isSynced = 0 AND sync_attempts < 5 ORDER BY createdAt ASC", null)
        
        if (cursor.moveToFirst()) {
            do {
                val localId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val item = EconomyUpdate(
                    id = localId,
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                    hdNumber = cursor.getString(cursor.getColumnIndexOrThrow("hdNumber")),
                    buildingName = cursor.getString(cursor.getColumnIndexOrThrow("buildingName")),
                    constructionCompany = cursor.getString(cursor.getColumnIndexOrThrow("constructionCompany")),
                    economiesCount = cursor.getInt(cursor.getColumnIndexOrThrow("economiesCount")),
                    floorsCount = cursor.getInt(cursor.getColumnIndexOrThrow("floorsCount")),
                    electricityMeterNumber = cursor.getString(cursor.getColumnIndexOrThrow("electricityMeterNumber")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("addedBy")),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    isSynced = false
                )
                list.add(localId to item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteSyncedEconomyUpdate(localId: String) {
        writableDatabase.delete("economy_updates", "id = ?", arrayOf(localId))
    }

    fun incrementSyncAttempt(table: String, id: String, lastError: String? = null) {
        val errorMsg = lastError ?: "Erro desconhecido"
        writableDatabase.execSQL(
            "UPDATE $table SET sync_attempts = sync_attempts + 1, last_error = ? WHERE id = ?", 
            arrayOf(errorMsg, id)
        )
    }

    fun resetSyncAttempts() {
        writableDatabase.execSQL("UPDATE customers SET sync_attempts = 0, last_error = NULL")
        writableDatabase.execSQL("UPDATE economy_updates SET sync_attempts = 0, last_error = NULL")
    }

    fun cacheUserProfile(id: String, username: String, fullName: String, cidadeId: String, isAdmin: Boolean, email: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("username", username)
            put("full_name", fullName)
            put("cidade_id", cidadeId)
            put("is_admin", if (isAdmin) 1 else 0)
            put("email", email)
        }
        writableDatabase.insertWithOnConflict("user_profile_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCachedUserProfile(username: String): UserProfile? {
        val cursor = readableDatabase.query(
            "user_profile_cache", 
            null, 
            "username = ?", 
            arrayOf(username.lowercase().trim()), 
            null, null, null
        )
        var profile: UserProfile? = null
        if (cursor.moveToFirst()) {
            profile = UserProfile(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                cargo = if (cursor.getInt(cursor.getColumnIndexOrThrow("is_admin")) == 1) "Administrador" else "Leiturista",
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: ""
            )
        }
        cursor.close()
        return profile
    }

    companion object {
        @Volatile
        private var instance: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: LocalDatabase(context.applicationContext).also { instance = it }
            }
        }

        private const val DATABASE_NAME = "sanitation_final_v5.db" // Visibility & Traceability
        private const val DATABASE_VERSION = 34

        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id TEXT PRIMARY KEY,
                name TEXT,
                matricula TEXT,
                digito_matricula TEXT,
                email TEXT,
                setor TEXT,
                quadra TEXT,
                celular TEXT,
                telefone_fixo TEXT,
                caixa_padrao TEXT,
                lacres_padronizados TEXT,
                hd_acessivel TEXT,
                veranista TEXT,
                possui_piscina TEXT,
                possui_caixa_agua TEXT,
                latitude REAL,
                longitude REAL,
                situacao_local TEXT,
                qtd_economias INTEGER,
                criado_em TEXT,
                capturado_em TEXT,
                adicionado_por TEXT,
                cidade_id TEXT,
                leiturista_id TEXT,
                date TEXT,
                qualidade TEXT,
                entrevistado_nome TEXT,
                entrevistado_cpf TEXT,
                entrevistado_mae TEXT,
                entrevistado_nascimento TEXT,
                entrevistado_sexo TEXT,
                entrevistado_apresentou_doc TEXT,
                entrevistado_qual_doc TEXT,
                proprietario_nome TEXT,
                proprietario_cpf TEXT,
                proprietario_mae TEXT,
                proprietario_nascimento TEXT,
                proprietario_sexo TEXT,
                proprietario_apresentou_doc TEXT,
                proprietario_qual_doc TEXT,
                locatario_nome TEXT,
                locatario_cpf TEXT,
                locatario_mae TEXT,
                locatario_nascimento TEXT,
                locatario_sexo TEXT,
                locatario_apresentou_doc TEXT,
                locatario_qual_doc TEXT,
                logradouro TEXT,
                numero TEXT,
                complemento TEXT,
                bairro TEXT,
                uf TEXT,
                cep TEXT,
                cidade TEXT,
                pavimento_rua TEXT,
                fonte_abastecimento TEXT,
                existe_rede_agua TEXT,
                observacao TEXT,
                grupo_sugerido TEXT,
                rota_sugerida TEXT,
                isSynced INTEGER DEFAULT 0,
                numero_hidrometro TEXT,
                pavimento_calcada TEXT,
                local_instalacao TEXT,
                acessibilidade TEXT,
                usa_agua_vizinho TEXT,
                beneficiario_social TEXT,
                possui_hidrometro TEXT,
                sync_attempts INTEGER DEFAULT 0,
                last_error TEXT
            )
        """

        // SÊNIOR PERFORMANCE INDEXES: Acelera faturamento, estatísticas e sincronização
        private const val CREATE_TABLE_STATS = "CREATE TABLE IF NOT EXISTS stats (id INTEGER PRIMARY KEY AUTOINCREMENT, record_value INTEGER, date_achieved TEXT)"
        private const val CREATE_TABLE_HISTORY = "CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, count INTEGER, date TEXT DEFAULT (date('now')))"
        private const val CREATE_TABLE_USER_CACHE = "CREATE TABLE IF NOT EXISTS user_profile_cache (id TEXT PRIMARY KEY, username TEXT, full_name TEXT, cidade_id TEXT, is_admin INTEGER, email TEXT)"

        private const val CREATE_TABLE_ECONOMY_UPDATES = """
            CREATE TABLE economy_updates (
                id TEXT PRIMARY KEY,
                cidade_id TEXT,
                leiturista_id TEXT,
                hdNumber TEXT,
                buildingName TEXT,
                constructionCompany TEXT,
                economiesCount INTEGER,
                floorsCount INTEGER,
                electricityMeterNumber TEXT,
                latitude REAL,
                longitude REAL,
                addedBy TEXT,
                createdAt TEXT,
                date TEXT,
                isSynced INTEGER DEFAULT 0,
                sync_attempts INTEGER DEFAULT 0,
                last_error TEXT
            )
        """

        // SÊNIOR PERFORMANCE INDEXES: Acelera faturamento, estatísticas e sincronização
        private const val IDX_CUSTOMERS_SYNC = "CREATE INDEX IF NOT EXISTS idx_customers_sync ON customers (isSynced, sync_attempts)"
        private const val IDX_ECONOMY_SYNC = "CREATE INDEX IF NOT EXISTS idx_economy_sync ON economy_updates (isSynced, sync_attempts)"
        private const val IDX_CUSTOMERS_CITY = "CREATE INDEX IF NOT EXISTS idx_customers_city ON customers (cidade_id)"
    }
}
