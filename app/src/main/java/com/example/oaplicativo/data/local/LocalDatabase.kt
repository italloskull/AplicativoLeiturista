package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.oaplicativo.model.Customer

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("LocalDatabase", "Migrando banco de $oldVersion para $newVersion")
        
        if (oldVersion < 21) {
            // BLINDAGEM DE EMERGÊNCIA: Adiciona colunas que podem estar faltando
            val columnsToAdd = listOf(
                "isSynced" to "INTEGER DEFAULT 0",
                "numero_hidrometro" to "TEXT",
                "local_instalacao" to "TEXT",
                "acessibilidade" to "TEXT"
            )
            
            for ((col, type) in columnsToAdd) {
                try {
                    db.execSQL("ALTER TABLE customers ADD COLUMN $col $type")
                    Log.d("LocalDatabase", "Coluna $col adicionada com sucesso.")
                } catch (e: Exception) {
                    Log.w("LocalDatabase", "Coluna $col já existe ou erro ao adicionar: ${e.message}")
                }
            }
        }
    }

    fun purgeOldRecords() {
        val db = writableDatabase
        db.delete("customers", "isSynced = 1", null)
    }

    fun updateRecordIfHigher(currentCount: Int) {
        val db = writableDatabase
        val personalRecord = getPersonalRecord()
        if (currentCount > personalRecord) {
            val values = ContentValues().apply {
                put("record_value", currentCount)
                put("date_achieved", System.currentTimeMillis().toString())
            }
            db.insertWithOnConflict("stats", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getPersonalRecord(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MAX(record_value) FROM stats", null)
        var record = 0
        if (cursor.moveToFirst()) {
            record = cursor.getInt(0)
        }
        cursor.close()
        return record
    }

    fun getTodayStats(): Map<String, Int> {
        val db = readableDatabase
        val stats = mutableMapOf<String, Int>()
        val cursor = db.rawQuery("SELECT type, count FROM history WHERE date = date('now')", null)
        while (cursor.moveToNext()) {
            stats[cursor.getString(0)] = cursor.getInt(1)
        }
        cursor.close()
        return stats
    }

    fun saveCustomerOffline(customer: Customer) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", customer.name)
            put("matricula", customer.registrationNumber)
            put("digito_matricula", customer.registrationDigit)
            put("email", customer.email)
            put("setor", customer.setor)
            put("quadra", customer.quadra)
            put("celular", customer.cellPhone)
            put("telefone_fixo", customer.landline)
            put("caixa_padrao", if (customer.isStandardMeasurementBox == true) 1 else 0)
            put("lacres_padronizados", if (customer.isStandardizedSeals == true) 1 else 0)
            put("hd_acessivel", if (customer.isHdAccessible == true) 1 else 0)
            put("veranista", if (customer.isVacationer == true) 1 else 0)
            put("possui_piscina", if (customer.possuiPiscina == true) 1 else 0)
            put("possui_caixa_agua", customer.possuiCaixaAgua)
            put("beneficiario_social", if (customer.beneficiarioSocial == true) 1 else 0)
            put("usa_agua_vizinho", if (customer.usaAguaVizinho == true) 1 else 0)
            put("possui_hidrometro", if (customer.possuiHidrometro == true) 1 else 0)
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
            put("entrevistado_apresentou_doc", if (customer.entrevistadoApresentouDoc == true) 1 else 0)
            put("entrevistado_qual_doc", customer.entrevistadoQualDoc)
            put("proprietario_nome", customer.proprietarioNome)
            put("proprietario_cpf", customer.proprietarioCpf)
            put("proprietario_mae", customer.proprietarioMae)
            put("proprietario_nascimento", customer.proprietarioNascimento)
            put("proprietario_sexo", customer.proprietarioSexo)
            put("proprietario_apresentou_doc", if (customer.proprietarioApresentouDoc == true) 1 else 0)
            put("proprietario_qual_doc", customer.proprietarioQual_doc)
            put("locatario_nome", customer.locatarioNome)
            put("locatario_cpf", customer.locatarioCpf)
            put("locatario_mae", customer.locatarioMae)
            put("locatario_nascimento", customer.locatarioNascimento)
            put("locatario_sexo", customer.locatarioSexo)
            put("locatario_apresentou_doc", if (customer.locatarioApresentouDoc == true) 1 else 0)
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
            put("hidrometro_proximo", customer.hidrometroProximo)
            put("fonte_abastecimento", customer.fonteAbastecimento)
            put("existe_rede_agua", if (customer.existeRedeAgua == true) 1 else 0)
            put("observacao", customer.observacao)
            put("grupo_sugerido", customer.grupoSugerido)
            put("isSynced", 0)
            put("numero_hidrometro", customer.numeroHidrometro)
            put("local_instalacao", customer.localInstalacao)
            put("acessibilidade", customer.acessibilidade)
        }
        db.insert("customers", null, values)
    }

    fun getPendingCustomers(): List<Pair<Int, Customer>> {
        val db = readableDatabase
        val list = mutableListOf<Pair<Int, Customer>>()
        val cursor = db.query("customers", null, "isSynced = 0", null, null, null, null)
        
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val customer = Customer(
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                registrationDigit = cursor.getString(cursor.getColumnIndexOrThrow("digito_matricula")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                setor = cursor.getString(cursor.getColumnIndexOrThrow("setor")),
                quadra = cursor.getString(cursor.getColumnIndexOrThrow("quadra")),
                cellPhone = cursor.getString(cursor.getColumnIndexOrThrow("celular")),
                landline = cursor.getString(cursor.getColumnIndexOrThrow("telefone_fixo")),
                isStandardMeasurementBox = cursor.getInt(cursor.getColumnIndexOrThrow("caixa_padrao")) == 1,
                isStandardizedSeals = cursor.getInt(cursor.getColumnIndexOrThrow("lacres_padronizados")) == 1,
                isHdAccessible = cursor.getInt(cursor.getColumnIndexOrThrow("hd_acessivel")) == 1,
                isVacationer = cursor.getInt(cursor.getColumnIndexOrThrow("veranista")) == 1,
                possuiPiscina = cursor.getInt(cursor.getColumnIndexOrThrow("possui_piscina")) == 1,
                possuiCaixaAgua = cursor.getString(cursor.getColumnIndexOrThrow("possui_caixa_agua")),
                beneficiarioSocial = cursor.getInt(cursor.getColumnIndexOrThrow("beneficiario_social")) == 1,
                usaAguaVizinho = cursor.getInt(cursor.getColumnIndexOrThrow("usa_agua_vizinho")) == 1,
                possuiHidrometro = cursor.getInt(cursor.getColumnIndexOrThrow("possui_hidrometro")) == 1,
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
                entrevistadoApresentouDoc = cursor.getInt(cursor.getColumnIndexOrThrow("entrevistado_apresentou_doc")) == 1,
                entrevistadoQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_qual_doc")),
                proprietarioNome = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_nome")),
                proprietarioCpf = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_cpf")),
                proprietarioMae = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_mae")),
                proprietarioNascimento = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_nascimento")),
                proprietarioSexo = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_sexo")),
                proprietarioApresentouDoc = cursor.getInt(cursor.getColumnIndexOrThrow("proprietario_apresentou_doc")) == 1,
                proprietarioQual_doc = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_qual_doc")),
                locatarioNome = cursor.getString(cursor.getColumnIndexOrThrow("locatario_nome")),
                locatarioCpf = cursor.getString(cursor.getColumnIndexOrThrow("locatario_cpf")),
                locatarioMae = cursor.getString(cursor.getColumnIndexOrThrow("locatario_mae")),
                locatarioNascimento = cursor.getString(cursor.getColumnIndexOrThrow("locatario_nascimento")),
                locatarioSexo = cursor.getString(cursor.getColumnIndexOrThrow("locatario_sexo")),
                locatarioApresentouDoc = cursor.getInt(cursor.getColumnIndexOrThrow("locatario_apresentou_doc")) == 1,
                locatarioQualDoc = cursor.getString(cursor.getColumnIndexOrThrow("locatario_qual_doc")),
                logradouro = cursor.getString(cursor.getColumnIndexOrThrow("logradouro")),
                numero = cursor.getString(cursor.getColumnIndexOrThrow("numero")),
                complemento = cursor.getString(cursor.getColumnIndexOrThrow("complemento")),
                bairro = cursor.getString(cursor.getColumnIndexOrThrow("bairro")),
                uf = cursor.getString(cursor.getColumnIndexOrThrow("uf")),
                cep = cursor.getString(cursor.getColumnIndexOrThrow("cep")),
                cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                pavimentoRua = cursor.getString(cursor.getColumnIndexOrThrow("pavimento_rua")),
                pavimentoCalcada = cursor.getString(cursor.getColumnIndexOrThrow("pavimento_calcada")),
                hidrometroProximo = cursor.getString(cursor.getColumnIndexOrThrow("hidrometro_proximo")),
                fonteAbastecimento = cursor.getString(cursor.getColumnIndexOrThrow("fonte_abastecimento")),
                existeRedeAgua = cursor.getInt(cursor.getColumnIndexOrThrow("existe_rede_agua")) == 1,
                observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao")),
                grupoSugerido = cursor.getString(cursor.getColumnIndexOrThrow("grupo_sugerido")),
                localInstalacao = cursor.getString(cursor.getColumnIndexOrThrow("local_instalacao")),
                acessibilidade = cursor.getString(cursor.getColumnIndexOrThrow("acessibilidade")),
                numeroHidrometro = cursor.getString(cursor.getColumnIndexOrThrow("numero_hidrometro")),
                isSynced = false
            )
            list.add(Pair(id, customer))
        }
        cursor.close()
        return list
    }

    fun deleteSyncedCustomer(localId: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("customers", "id = ?", arrayOf(localId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRecadastroStats(): Pair<Int, Int> {
        val db = readableDatabase
        var total = 0
        var pending = 0
        
        val cursorTotal = db.rawQuery("SELECT COUNT(*) FROM customers", null)
        if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
        cursorTotal.close()
        
        val cursorPending = db.rawQuery("SELECT COUNT(*) FROM customers WHERE isSynced = 0", null)
        if (cursorPending.moveToFirst()) pending = cursorPending.getInt(0)
        cursorPending.close()
        
        return Pair(total, pending)
    }

    fun getEconomyStats(): Pair<Int, Int> {
        val db = readableDatabase
        var total = 0
        var pending = 0
        
        try {
            val cursorTotal = db.rawQuery("SELECT COUNT(*) FROM economy_updates", null)
            if (cursorTotal.moveToFirst()) total = cursorTotal.getInt(0)
            cursorTotal.close()
            
            val cursorPending = db.rawQuery("SELECT COUNT(*) FROM economy_updates WHERE isSynced = 0", null)
            if (cursorPending.moveToFirst()) pending = cursorPending.getInt(0)
            cursorPending.close()
        } catch (_: Exception) {
            return Pair(0, 0)
        }
        
        return Pair(total, pending)
    }

    companion object {
        private const val DATABASE_NAME = "sanitation_local_v13.db"
        private const val DATABASE_VERSION = 21

        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                matricula TEXT,
                digito_matricula TEXT,
                email TEXT,
                setor TEXT,
                quadra TEXT,
                celular TEXT,
                telefone_fixo TEXT,
                caixa_padrao INTEGER,
                lacres_padronizados INTEGER,
                hd_acessivel INTEGER,
                veranista INTEGER,
                possui_piscina INTEGER,
                possui_caixa_agua TEXT,
                beneficiario_social INTEGER,
                usa_agua_vizinho INTEGER,
                possui_hidrometro INTEGER,
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
                entrevistado_apresentou_doc INTEGER,
                entrevistado_qual_doc TEXT,
                proprietario_nome TEXT,
                proprietario_cpf TEXT,
                proprietario_mae TEXT,
                proprietario_nascimento TEXT,
                proprietario_sexo TEXT,
                proprietario_apresentou_doc INTEGER,
                proprietario_qual_doc TEXT,
                locatario_nome TEXT,
                locatario_cpf TEXT,
                locatario_mae TEXT,
                locatario_nascimento TEXT,
                locatario_sexo TEXT,
                locatario_apresentou_doc INTEGER,
                locatario_qual_doc TEXT,
                logradouro TEXT,
                numero TEXT,
                complemento TEXT,
                bairro TEXT,
                uf TEXT,
                cep TEXT,
                cidade TEXT,
                pavimento_rua TEXT,
                pavimento_calcada TEXT,
                hidrometro_proximo TEXT,
                fonte_abastecimento TEXT,
                existe_rede_agua INTEGER,
                observacao TEXT,
                grupo_sugerido TEXT,
                isSynced INTEGER DEFAULT 0,
                numero_hidrometro TEXT,
                local_instalacao TEXT,
                acessibilidade TEXT
            )
        """

        private const val CREATE_TABLE_STATS = """
            CREATE TABLE IF NOT EXISTS stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                record_value INTEGER,
                date_achieved TEXT
            )
        """

        private const val CREATE_TABLE_HISTORY = """
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                count INTEGER,
                date TEXT DEFAULT (date('now'))
            )
        """
    }
}
