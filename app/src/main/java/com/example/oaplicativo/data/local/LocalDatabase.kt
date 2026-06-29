package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.util.normalizeQuality
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    private val dbLock = Any()

    override fun onCreate(db: SQLiteDatabase) {
        synchronized(dbLock) {
            db.execSQL(CREATE_TABLE_CUSTOMERS)
            db.execSQL(CREATE_TABLE_ECONOMY_UPDATES)
            db.execSQL(CREATE_TABLE_STATS)
            db.execSQL(CREATE_TABLE_HISTORY)
            db.execSQL(CREATE_TABLE_USER_CACHE)
            db.execSQL(IDX_CUSTOMERS_SYNC)
            db.execSQL(IDX_ECONOMY_SYNC)
            db.execSQL(IDX_CUSTOMERS_CITY)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        synchronized(dbLock) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL("DROP TABLE IF EXISTS grandes_empreendimentos")
            db.execSQL("DROP TABLE IF EXISTS stats")
            db.execSQL("DROP TABLE IF EXISTS history")
            db.execSQL("DROP TABLE IF EXISTS user_profile_cache")
            onCreate(db)
        }
    }

    fun saveCustomerOffline(customer: Customer): Int {
        val db = writableDatabase
        synchronized(dbLock) {
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
            
            Log.d("debugs", "💾 [SQLITE] Gravando Recadastro: ${customer.name} | Cidade: ${customer.cidade}")
            return db.insertWithOnConflict("customers", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt()
        }
    }

    fun getPendingCustomers(cityName: String? = null, bypass: Boolean = false): List<Pair<String, Customer>> {
        val list = mutableListOf<Pair<String, Customer>>()
        val selection = if (bypass || cityName == null) "isSynced = 0 AND sync_attempts < 5" 
                        else "isSynced = 0 AND sync_attempts < 5 AND cidade = ?"
        val selectionArgs = if (bypass || cityName == null) null else arrayOf(cityName)

        val cursor = readableDatabase.query("customers", null, selection, selectionArgs, null, null, "date ASC")
        
        fun getStrOrNull(col: String): String? {
            val idx = cursor.getColumnIndex(col)
            return if (idx != -1 && !cursor.isNull(idx)) cursor.getString(idx) else null
        }

        while (cursor.moveToNext()) {
            val localId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val customer = Customer(
                id = localId,
                cidadeId = getStrOrNull("cidade_id"),
                leituristaId = getStrOrNull("leiturista_id"),
                name = getStrOrNull("name"),
                registrationNumber = getStrOrNull("matricula"),
                registrationDigit = getStrOrNull("digito_matricula"),
                email = getStrOrNull("email"),
                celular = getStrOrNull("celular"),
                isStandardMeasurementBox = getStrOrNull("caixa_padrao"),
                isStandardizedSeals = getStrOrNull("lacres_padronizados"),
                isHdAccessible = getStrOrNull("hd_acessivel"),
                isVacationer = getStrOrNull("veranista"),
                possuiPiscina = getStrOrNull("possui_piscina"),
                possuiCaixaAgua = getStrOrNull("possui_caixa_agua"),
                latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                locationStatus = getStrOrNull("situacao_local"),
                economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                addedBy = getStrOrNull("adicionado_por"),
                capturedAt = getStrOrNull("capturado_em"),
                date = getStrOrNull("date"),
                quality = getStrOrNull("qualidade"),
                entrevistadoNome = getStrOrNull("entrevistado_nome"),
                entrevistadoCpf = getStrOrNull("entrevistado_cpf"),
                entrevistadoMae = getStrOrNull("entrevistado_mae"),
                entrevistadoNascimento = getStrOrNull("entrevistado_nascimento"),
                entrevistadoSexo = getStrOrNull("entrevistado_sexo"),
                entrevistadoApresentouDoc = getStrOrNull("entrevistado_apresentou_doc"),
                entrevistadoQualDoc = getStrOrNull("entrevistado_qual_doc"),
                logradouro = getStrOrNull("logradouro"),
                numero = getStrOrNull("numero"),
                complemento = getStrOrNull("complemento"),
                bairro = getStrOrNull("bairro"),
                cidade = getStrOrNull("cidade"),
                uf = getStrOrNull("uf"),
                cep = getStrOrNull("cep"),
                pavimentoRua = getStrOrNull("pavimento_rua"),
                pavimentoCalcada = getStrOrNull("pavimento_calcada"),
                fonteAbastecimento = getStrOrNull("fonte_abastecimento"),
                existeRedeAgua = getStrOrNull("existe_rede_agua"),
                observacao = getStrOrNull("observacao"),
                beneficiarioSocial = getStrOrNull("beneficiario_social"),
                usaAguaVizinho = getStrOrNull("usa_agua_vizinho"),
                possuiHidrometro = getStrOrNull("possui_hidrometro"),
                grupoSugerido = getStrOrNull("grupo_sugerido"),
                setor = getStrOrNull("setor"),
                quadra = getStrOrNull("quadra"),
                localInstalacao = getStrOrNull("local_instalacao"),
                acessibilidade = getStrOrNull("acessibilidade"),
                rotaSugerida = getStrOrNull("rota_sugerida"),
                numeroHidrometro = getStrOrNull("numero_hidrometro"),
                isSynced = false
            )
            list.add(localId to customer)
        }
        cursor.close()
        return list
    }

    fun deleteSyncedCustomer(id: String) {
        writableDatabase.delete("customers", "id = ?", arrayOf(id))
    }

    fun saveEconomyUpdateOffline(item: EconomyUpdate) = synchronized(dbLock) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("id", item.id ?: java.util.UUID.randomUUID().toString())
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
                put("grupo_sugerido", item.grupoSugerido)
                put("rota_sugerida", item.rotaSugerida)
                put("adicionado_por", item.addedBy)
                put("createdAt", item.createdAt)
                put("date", item.date)
                put("qualidade", "Boa") 
                put("isSynced", 0)
            }
            db.insertWithOnConflict("grandes_empreendimentos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
            Log.d("debugs", "✅ [SQLITE] GE Gravado: ${item.buildingName} | Cidade: ${item.cidade}")
        } catch (e: Exception) {
            Log.e("debugs", "❌ [SQLITE] Falha GE: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingEconomyUpdates(cityName: String? = null, bypass: Boolean = false): List<Pair<String, EconomyUpdate>> {
        val list = mutableListOf<Pair<String, EconomyUpdate>>()
        val db = readableDatabase
        
        val selection = if (bypass || cityName == null) "isSynced = 0 AND sync_attempts < 5"
                        else "isSynced = 0 AND sync_attempts < 5 AND cidade = ?" 
        val selectionArgs = if (bypass || cityName == null) null else arrayOf(cityName)

        val cursor = db.query("grandes_empreendimentos", null, selection, selectionArgs, null, null, "createdAt ASC")
        while (cursor.moveToNext()) {
            val localId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val item = EconomyUpdate(
                id = localId,
                leituristaId = cursor.getString(cursor.getColumnIndexOrThrow("leiturista_id")),
                hdNumber = cursor.getString(cursor.getColumnIndexOrThrow("numero_hd")),
                buildingName = cursor.getString(cursor.getColumnIndexOrThrow("nome_edificio")),
                constructionCompany = cursor.getString(cursor.getColumnIndexOrThrow("construtora")),
                economiesCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_economias"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_economias")),
                floorsCount = if (cursor.isNull(cursor.getColumnIndexOrThrow("qtd_pavimentos"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("qtd_pavimentos")),
                electricityMeterNumber = cursor.getString(cursor.getColumnIndexOrThrow("medidor_energia")),
                latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                rotaSugerida = cursor.getString(cursor.getColumnIndexOrThrow("rota_sugerida")),
                addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                isSynced = false
            )
            list.add(localId to item)
        }
        cursor.close()
        return list
    }

    fun deleteSyncedEconomyUpdate(localId: String) {
        writableDatabase.delete("grandes_empreendimentos", "id = ?", arrayOf(localId))
    }

    fun getTodayStats(cityName: String? = null, bypass: Boolean = false): Map<String, Int> {
        val stats = mutableMapOf("Boa" to 0, "Regular" to 0, "Ruim" to 0, "Total" to 0)
        val today = ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        try {
            val selectionArgs = if (bypass || cityName == null) arrayOf(today) else arrayOf(today, cityName)
            
            // 1. Clientes
            val queryCustomers = if (bypass || cityName == null) "SELECT qualidade, COUNT(*) FROM customers WHERE date = ? GROUP BY qualidade"
                                 else "SELECT qualidade, COUNT(*) FROM customers WHERE date = ? AND cidade = ? GROUP BY qualidade"

            val cursorCustomers = readableDatabase.rawQuery(queryCustomers, selectionArgs)
            while (cursorCustomers.moveToNext()) {
                val q = cursorCustomers.getString(0) ?: "Ruim"
                val count = cursorCustomers.getInt(1)
                val norm = q.normalizeQuality()
                stats[norm] = (stats[norm] ?: 0) + count
                stats["Total"] = (stats["Total"] ?: 0) + count
            }
            cursorCustomers.close()
            
            // 2. Grandes Empreendimentos
            val queryEconomy = if (bypass || cityName == null) "SELECT qualidade, COUNT(*) FROM grandes_empreendimentos WHERE date = ? GROUP BY qualidade"
                               else "SELECT qualidade, COUNT(*) FROM grandes_empreendimentos WHERE date = ? AND cidade = ? GROUP BY qualidade"
            
            val cursorEconomy = readableDatabase.rawQuery(queryEconomy, selectionArgs)
            while (cursorEconomy.moveToNext()) {
                val q = cursorEconomy.getString(0) ?: "Boa"
                val count = cursorEconomy.getInt(1)
                val norm = q.normalizeQuality()
                stats[norm] = (stats[norm] ?: 0) + count
                stats["Total"] = (stats["Total"] ?: 0) + count
            }
            cursorEconomy.close()
        } catch (e: Exception) {
            Log.e("debugs", "❌ [SQLITE] Erro Stats: ${e.message}")
        }
        return stats
    }

    fun getRecadastroStats(cityName: String? = null, bypass: Boolean = false): Pair<Int, Int> {
        var total = 0; var pending = 0
        try {
            val args = if (bypass || cityName == null) null else arrayOf(cityName)
            val qTotal = if (bypass || cityName == null) "SELECT COUNT(*) FROM customers" else "SELECT COUNT(*) FROM customers WHERE cidade = ?"
            val cTotal = readableDatabase.rawQuery(qTotal, args)
            if (cTotal.moveToFirst()) total = cTotal.getInt(0)
            cTotal.close()

            val qPend = if (bypass || cityName == null) "SELECT COUNT(*) FROM customers WHERE isSynced = 0" else "SELECT COUNT(*) FROM customers WHERE isSynced = 0 AND cidade = ?"
            val cPend = readableDatabase.rawQuery(qPend, args)
            if (cPend.moveToFirst()) pending = cPend.getInt(0)
            cPend.close()
        } catch (_: Exception) {}
        return Pair(total, pending)
    }

    fun getEconomyStats(cityName: String? = null, bypass: Boolean = false): Pair<Int, Int> {
        var total = 0; var pending = 0
        try {
            val args = if (bypass || cityName == null) null else arrayOf(cityName)
            val qTotal = if (bypass || cityName == null) "SELECT COUNT(*) FROM grandes_empreendimentos" else "SELECT COUNT(*) FROM grandes_empreendimentos WHERE cidade = ?"
            val cTotal = readableDatabase.rawQuery(qTotal, args)
            if (cTotal.moveToFirst()) total = cTotal.getInt(0)
            cTotal.close()

            val qPend = if (bypass || cityName == null) "SELECT COUNT(*) FROM grandes_empreendimentos WHERE isSynced = 0" else "SELECT COUNT(*) FROM grandes_empreendimentos WHERE isSynced = 0 AND cidade = ?"
            val cPend = readableDatabase.rawQuery(qPend, args)
            if (cPend.moveToFirst()) pending = cPend.getInt(0)
            cPend.close()
        } catch (_: Exception) {}
        return Pair(total, pending)
    }

    fun incrementSyncAttempt(tableName: String, id: String, error: String?) {
        val sql = "UPDATE $tableName SET sync_attempts = sync_attempts + 1, last_error = ? WHERE id = ?"
        writableDatabase.execSQL(sql, arrayOf(error, id))
    }

    fun resetSyncAttempts() {
        writableDatabase.execSQL("UPDATE customers SET sync_attempts = 0")
        writableDatabase.execSQL("UPDATE grandes_empreendimentos SET sync_attempts = 0")
    }

    fun updateRecordIfHigher(value: Int) {
        val current = getPersonalRecord()
        if (value > current) {
            val values = ContentValues().apply { put("record_value", value); put("date_achieved", ZonedDateTime.now().toString()) }
            writableDatabase.insert("stats", null, values)
        }
    }

    fun getPersonalRecord(): Int {
        val cursor = readableDatabase.query("stats", arrayOf("MAX(record_value)"), null, null, null, null, null)
        var record = 0
        if (cursor.moveToFirst()) record = cursor.getInt(0)
        cursor.close(); return record
    }

    fun cacheUserProfile(id: String, user: String, name: String, cidade: String, isAdmin: Boolean, email: String) {
        val values = ContentValues().apply { put("id", id); put("username", user); put("full_name", name); put("cidade_id", cidade); put("is_admin", if(isAdmin) 1 else 0); put("email", email) }
        writableDatabase.insertWithOnConflict("user_profile_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCachedUserProfile(username: String): UserProfile? {
        val cursor = readableDatabase.query("user_profile_cache", null, "username = ?", arrayOf(username), null, null, null)
        if (cursor.moveToFirst()) {
            val profile = UserProfile(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cargo = if (cursor.getInt(cursor.getColumnIndexOrThrow("is_admin")) == 1) "administrador" else "usuário",
                cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id"))
            )
            cursor.close(); return profile
        }
        cursor.close(); return null
    }

    companion object {
        @Volatile private var instance: LocalDatabase? = null
        fun getInstance(context: Context): LocalDatabase {
            return instance ?: synchronized(this) { instance ?: LocalDatabase(context.applicationContext).also { instance = it } }
        }
        private const val DATABASE_NAME = "sanitation_final_v6.db"
        private const val DATABASE_VERSION = 37
        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE IF NOT EXISTS customers (
                id TEXT PRIMARY KEY, cidade_id TEXT, leiturista_id TEXT, name TEXT, matricula TEXT, digito_matricula TEXT, email TEXT, celular TEXT,
                caixa_padrao TEXT, lacres_padronizados TEXT, hd_acessivel TEXT, veranista TEXT, possui_piscina TEXT, possui_caixa_agua TEXT,
                latitude REAL, longitude REAL, situacao_local TEXT, qtd_economias INTEGER, adicionado_por TEXT, capturado_em TEXT, date TEXT,
                qualidade TEXT, entrevistado_nome TEXT, entrevistado_cpf TEXT, entrevistado_mae TEXT, entrevistado_nascimento TEXT, entrevistado_sexo TEXT,
                entrevistado_apresentou_doc TEXT, entrevistado_qual_doc TEXT, logradouro TEXT, numero TEXT, complemento TEXT, bairro TEXT, cidade TEXT,
                uf TEXT, cep TEXT, pavimento_rua TEXT, pavimento_calcada TEXT, fonte_abastecimento TEXT, existe_rede_agua TEXT, observacao TEXT,
                beneficiario_social TEXT, usa_agua_vizinho TEXT, possui_hidrometro TEXT, grupo_sugerido TEXT, setor TEXT, quadra TEXT,
                local_instalacao TEXT, acessibilidade TEXT, rota_sugerida TEXT, numero_hidrometro TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT
            )"""
        private const val CREATE_TABLE_STATS = "CREATE TABLE IF NOT EXISTS stats (id INTEGER PRIMARY KEY AUTOINCREMENT, record_value INTEGER, date_achieved TEXT)"
        private const val CREATE_TABLE_HISTORY = "CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, count INTEGER, date TEXT DEFAULT (date('now')))"
        private const val CREATE_TABLE_USER_CACHE = "CREATE TABLE IF NOT EXISTS user_profile_cache (id TEXT PRIMARY KEY, username TEXT, full_name TEXT, cidade_id TEXT, is_admin INTEGER, email TEXT)"
        private const val CREATE_TABLE_ECONOMY_UPDATES = """
            CREATE TABLE IF NOT EXISTS grandes_empreendimentos (
                id TEXT PRIMARY KEY, leiturista_id TEXT, numero_hd TEXT, nome_edificio TEXT, construtora TEXT, qtd_economias INTEGER, qtd_pavimentos INTEGER,
                medidor_energia TEXT, latitude REAL, longitude REAL, cidade TEXT, grupo_sugerido TEXT, rota_sugerida TEXT, adicionado_por TEXT,
                createdAt TEXT, date TEXT, qualidade TEXT, isSynced INTEGER DEFAULT 0, sync_attempts INTEGER DEFAULT 0, last_error TEXT
            )"""
        private const val IDX_CUSTOMERS_SYNC = "CREATE INDEX IF NOT EXISTS idx_customers_sync ON customers (isSynced, sync_attempts)"
        private const val IDX_ECONOMY_SYNC = "CREATE INDEX IF NOT EXISTS idx_economy_sync ON grandes_empreendimentos (isSynced, sync_attempts)"
        private const val IDX_CUSTOMERS_CITY = "CREATE INDEX IF NOT EXISTS idx_customers_city ON customers (cidade)"
    }
}
