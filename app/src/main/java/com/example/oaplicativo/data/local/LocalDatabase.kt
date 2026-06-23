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
import kotlinx.coroutines.sync.Mutex
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // SÊNIOR FIX: Mutex de controle de concorrência global para evitar "Database is locked"
    private val dbLock = Any()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
        db.execSQL(CREATE_TABLE_USER_CACHE)
        db.execSQL(CREATE_TABLE_ECONOMY_UPDATES)
        db.execSQL(IDX_CUSTOMERS_SYNC)
        db.execSQL(IDX_ECONOMY_SYNC)
        db.execSQL(IDX_CUSTOMERS_CITY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL("DROP TABLE IF EXISTS economy_updates")
            onCreate(db)
        }
    }

    fun saveCustomerOffline(customer: Customer) = synchronized(dbLock) {
        val db = writableDatabase
        db.beginTransaction()
        try {
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
                put("criado_em", customer.createdAt)
                put("adicionado_por", customer.addedBy)
                put("capturado_em", customer.capturedAt)
                put("sincronizado_em", customer.synchronizedAt)
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
                put("rota_sugerida", customer.rotaSugerida)
                put("numero_hidrometro", customer.numeroHidrometro)
                put("isSynced", 0)
            }
            db.insertWithOnConflict("customers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
            Log.d("LocalDB", "✅ Recadastro salvo. Qualidade: ${customer.quality} | Data: ${customer.date}")
        } catch (e: Exception) {
            Log.e("LocalDB", "❌ ERRO CRÍTICO AO SALVAR RECADASTRO", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingCustomers(): List<Pair<String, Customer>> {
        val list = mutableListOf<Pair<String, Customer>>()
        val cursor = readableDatabase.query("customers", null, "isSynced = 0 AND sync_attempts < 5", null, null, null, "criado_em ASC")
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            
            fun getStrOrNull(column: String): String? {
                val idx = cursor.getColumnIndexOrThrow(column)
                return if (cursor.isNull(idx)) null else cursor.getString(idx)
            }

            val customer = Customer(
                id = id,
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
                createdAt = getStrOrNull("criado_em"),
                addedBy = getStrOrNull("adicionado_por"),
                capturedAt = getStrOrNull("capturado_em"),
                synchronizedAt = getStrOrNull("sincronizado_em"),
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
                rotaSugerida = getStrOrNull("rota_sugerida"),
                numeroHidrometro = getStrOrNull("numero_hidrometro"),
                isSynced = false
            )
            list.add(Pair(id, customer))
        }
        cursor.close()
        return list
    }

    fun deleteSyncedCustomer(localId: String) {
        writableDatabase.delete("customers", "id = ?", arrayOf(localId))
    }

    fun saveEconomyUpdateOffline(item: EconomyUpdate) = synchronized(dbLock) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
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
            db.insertWithOnConflict("economy_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
            Log.d("LocalDB", "✅ Economia salva. Data: ${item.date}")
        } catch (e: Exception) {
            Log.e("LocalDB", "❌ ERRO AO SALVAR ECONOMIA", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingEconomyUpdates(): List<Pair<String, EconomyUpdate>> {
        val list = mutableListOf<Pair<String, EconomyUpdate>>()
        val db = readableDatabase
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

    fun getRecadastroStats(): Pair<Int, Int> {
        var total = 0
        var pending = 0
        try {
            val cursorTotal = readableDatabase.rawQuery("SELECT COUNT(*) FROM customers", null)
            if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
            cursorTotal.close()
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
            val cursorTotal = readableDatabase.rawQuery("SELECT COUNT(*) FROM economy_updates", null)
            if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
            cursorTotal.close()
            val cursorPending = readableDatabase.rawQuery("SELECT COUNT(*) FROM economy_updates WHERE isSynced = 0 AND sync_attempts < 5", null)
            if (cursorPending.moveToFirst()) pending = cursorPending.getInt(0)
            cursorPending.close()
        } catch (_: Exception) {}
        return Pair(total, pending)
    }

    fun getTodayStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val today = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        
        Log.d("LocalDB", "📊 Buscando estatísticas para a data: $today")
        
        try {
            // Clientes (Agrupados por Qualidade)
            val cursorCustomers = readableDatabase.rawQuery(
                "SELECT qualidade, COUNT(*) FROM customers WHERE date = ? GROUP BY qualidade", 
                arrayOf(today)
            )
            
            var total = 0
            while (cursorCustomers.moveToNext()) {
                val q = cursorCustomers.getString(0)
                val c = cursorCustomers.getInt(1)
                // SÊNIOR PERF: Usando Extension Function para normalização centralizada
                val normalizedQ = q.normalizeQuality()
                stats[normalizedQ] = (stats[normalizedQ] ?: 0) + c
                total += c
            }
            cursorCustomers.close()
            
            // Economias (Sempre contam como Boa)
            val cursorEconomy = readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM economy_updates WHERE date = ?", 
                arrayOf(today)
            )
            if (cursorEconomy.moveToFirst()) {
                val countEcon = cursorEconomy.getInt(0)
                stats["Boa"] = (stats["Boa"] ?: 0) + countEcon
                total += countEcon
            }
            cursorEconomy.close()

            stats["Total"] = total
            Log.d("LocalDB", "📊 Estatísticas Consolidadas: $stats")
        } catch (e: Exception) {
            Log.e("LocalDB", "Erro ao calcular estatísticas", e)
        }
        return stats
    }

    fun updateRecordIfHigher(current: Int) {
        val db = writableDatabase
        val existing = getPersonalRecord()
        if (current > existing) {
            db.execSQL("INSERT OR REPLACE INTO stats (id, record_value, date_achieved) VALUES (1, ?, date('now'))", arrayOf(current))
        }
    }

    fun getPersonalRecord(): Int {
        val cursor = readableDatabase.rawQuery("SELECT record_value FROM stats WHERE id = 1", null)
        var res = 0
        if (cursor.moveToFirst()) res = cursor.getInt(0)
        cursor.close()
        return res
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
        val cursor = readableDatabase.query("user_profile_cache", null, "username = ?", arrayOf(username), null, null, null)
        var profile: UserProfile? = null
        if (cursor.moveToFirst()) {
            profile = UserProfile(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cargo = if (cursor.getInt(cursor.getColumnIndexOrThrow("is_admin")) == 1) "administrador" else "usuário",
                cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id"))
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

        private const val DATABASE_NAME = "sanitation_final_v6.db"
        private const val DATABASE_VERSION = 35

        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id TEXT PRIMARY KEY,
                cidade_id TEXT,
                leiturista_id TEXT,
                name TEXT,
                matricula TEXT,
                digito_matricula TEXT,
                email TEXT,
                celular TEXT,
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
                adicionado_por TEXT,
                capturado_em TEXT,
                sincronizado_em TEXT,
                date TEXT,
                qualidade TEXT,
                entrevistado_nome TEXT,
                entrevistado_cpf TEXT,
                entrevistado_mae TEXT,
                entrevistado_nascimento TEXT,
                entrevistado_sexo TEXT,
                entrevistado_apresentou_doc TEXT,
                entrevistado_qual_doc TEXT,
                logradouro TEXT,
                numero TEXT,
                complemento TEXT,
                bairro TEXT,
                cidade TEXT,
                uf TEXT,
                cep TEXT,
                pavimento_rua TEXT,
                pavimento_calcada TEXT,
                fonte_abastecimento TEXT,
                existe_rede_agua TEXT,
                observacao TEXT,
                beneficiario_social TEXT,
                usa_agua_vizinho TEXT,
                possui_hidrometro TEXT,
                grupo_sugerido TEXT,
                setor TEXT,
                quadra TEXT,
                rota_sugerida TEXT,
                numero_hidrometro TEXT,
                isSynced INTEGER DEFAULT 0,
                sync_attempts INTEGER DEFAULT 0,
                last_error TEXT
            )
        """

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

        private const val IDX_CUSTOMERS_SYNC = "CREATE INDEX IF NOT EXISTS idx_customers_sync ON customers (isSynced, sync_attempts)"
        private const val IDX_ECONOMY_SYNC = "CREATE INDEX IF NOT EXISTS idx_economy_sync ON economy_updates (isSynced, sync_attempts)"
        private const val IDX_CUSTOMERS_CITY = "CREATE INDEX IF NOT EXISTS idx_customers_city ON customers (cidade_id)"
    }
}
