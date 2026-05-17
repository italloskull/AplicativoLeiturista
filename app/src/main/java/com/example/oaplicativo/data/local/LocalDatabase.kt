package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.oaplicativo.model.Customer
import java.time.LocalDate

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_STATS)
        db.execSQL(CREATE_TABLE_HISTORY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 13) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL("DROP TABLE IF EXISTS stats")
            db.execSQL("DROP TABLE IF EXISTS history")
            onCreate(db)
        } else {
            // Adição robusta de colunas para garantir paridade total (v19)
            val columnsToAdd = listOf(
                "setor" to "TEXT",
                "quadra" to "TEXT",
                "digito_matricula" to "TEXT",
                "email" to "TEXT",
                "caixa_padrao" to "INTEGER",
                "lacres_padronizados" to "INTEGER",
                "hd_acessivel" to "INTEGER",
                "veranista" to "INTEGER",
                "possui_piscina" to "INTEGER",
                "possui_caixa_agua" to "TEXT",
                "beneficiario_social" to "INTEGER",
                "usa_agua_vizinho" to "INTEGER",
                "possui_hidrometro" to "INTEGER",
                "situacao_local" to "TEXT",
                "qtd_economias" to "INTEGER",
                "entrevistado_mae" to "TEXT",
                "entrevistado_nascimento" to "TEXT",
                "entrevistado_sexo" to "TEXT",
                "entrevistado_apresentou_doc" to "INTEGER",
                "entrevistado_qual_doc" to "TEXT",
                "proprietario_cpf" to "TEXT",
                "proprietario_mae" to "TEXT",
                "proprietario_nascimento" to "TEXT",
                "proprietario_sexo" to "TEXT",
                "proprietario_apresentou_doc" to "INTEGER",
                "proprietario_qual_doc" to "TEXT",
                "locatario_cpf" to "TEXT",
                "locatario_mae" to "TEXT",
                "locatario_nascimento" to "TEXT",
                "locatario_sexo" to "TEXT",
                "locatario_apresentou_doc" to "INTEGER",
                "locatario_qual_doc" to "TEXT",
                "logradouro" to "TEXT",
                "numero" to "TEXT",
                "complemento" to "TEXT",
                "bairro" to "TEXT",
                "uf" to "TEXT",
                "cep" to "TEXT",
                "pavimento_rua" to "TEXT",
                "pavimento_calcada" to "TEXT",
                "hidrometro_proximo" to "TEXT",
                "fonte_abastecimento" to "TEXT",
                "existe_rede_agua" to "INTEGER",
                "observacao" to "TEXT",
                "grupo_sugerido" to "TEXT"
            )

            for ((col, type) in columnsToAdd) {
                try {
                    db.execSQL("ALTER TABLE customers ADD COLUMN $col $type")
                } catch (e: Exception) {
                    // Ignora se a coluna já existir
                }
            }

            if (oldVersion < 17) {
                try { db.execSQL(CREATE_TABLE_HISTORY) } catch (e: Exception) {}
            }
        }
    }

    fun purgeOldRecords() {
        writableDatabase.delete("customers", null, null)
    }

    fun updateRecordIfHigher(currentCount: Int) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT record_value FROM stats WHERE stat_key = 'daily_record'", null)
        var highest = 0
        if (cursor.moveToFirst()) highest = cursor.getInt(0)
        cursor.close()

        if (currentCount > highest) {
            val values = ContentValues().apply {
                put("stat_key", "daily_record")
                put("record_value", currentCount)
                put("last_updated", LocalDate.now().toString())
            }
            db.insertWithOnConflict("stats", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getPersonalRecord(): Int {
        val cursor = readableDatabase.rawQuery("SELECT record_value FROM stats WHERE stat_key = 'daily_record'", null)
        val record = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return record
    }

    fun getTodayStats(): Map<String, Int> {
        val stats = mutableMapOf("Boa" to 0, "Regular" to 0, "Ruim" to 0, "Total" to 0)
        val today = LocalDate.now().toString()
        try {
            val cursor = readableDatabase.rawQuery(
                "SELECT qualidade, COUNT(*) FROM history WHERE date = ? GROUP BY qualidade", 
                arrayOf(today)
            )
            while (cursor.moveToNext()) {
                val q = cursor.getString(0) ?: "Ruim"
                val count = cursor.getInt(1)
                stats[q] = count
                stats["Total"] = (stats["Total"] ?: 0) + count
            }
            cursor.close()
        } catch (_: Exception) { }
        return stats
    }

    fun saveCustomerOffline(customer: Customer) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("name", customer.name)
                put("matricula", customer.registrationNumber)
                put("digito_matricula", customer.registrationDigit)
                put("email", customer.email)
                put("setor", customer.setor)
                put("quadra", customer.quadra)
                put("celular", customer.cellPhone)
                put("telefone_fixo", customer.landline)
                
                // OTIMIZAÇÃO SÊNIOR: Preserva o NULL real para o estado "Não Informado"
                if (customer.isStandardMeasurementBox == null) putNull("caixa_padrao") else put("caixa_padrao", if (customer.isStandardMeasurementBox) 1 else 0)
                if (customer.isStandardizedSeals == null) putNull("lacres_padronizados") else put("lacres_padronizados", if (customer.isStandardizedSeals) 1 else 0)
                if (customer.isHdAccessible == null) putNull("hd_acessivel") else put("hd_acessivel", if (customer.isHdAccessible) 1 else 0)
                if (customer.isVacationer == null) putNull("veranista") else put("veranista", if (customer.isVacationer) 1 else 0)
                
                if (customer.possuiPiscina == null) putNull("possui_piscina") else put("possui_piscina", if (customer.possuiPiscina) 1 else 0)
                put("possui_caixa_agua", customer.possuiCaixaAgua)
                if (customer.beneficiarioSocial == null) putNull("beneficiario_social") else put("beneficiario_social", if (customer.beneficiarioSocial) 1 else 0)
                if (customer.usaAguaVizinho == null) putNull("usa_agua_vizinho") else put("usa_agua_vizinho", if (customer.usaAguaVizinho) 1 else 0)
                if (customer.possuiHidrometro == null) putNull("possui_hidrometro") else put("possui_hidrometro", if (customer.possuiHidrometro) 1 else 0)
                if (customer.existeRedeAgua == null) putNull("existe_rede_agua") else put("existe_rede_agua", if (customer.existeRedeAgua) 1 else 0)
                
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
                if (customer.entrevistadoApresentouDoc == null) putNull("entrevistado_apresentou_doc") else put("entrevistado_apresentou_doc", if (customer.entrevistadoApresentouDoc) 1 else 0)
                put("entrevistado_qual_doc", customer.entrevistadoQualDoc)
                
                put("proprietario_nome", customer.proprietarioNome)
                put("proprietario_cpf", customer.proprietarioCpf)
                put("proprietario_mae", customer.proprietarioMae)
                put("proprietario_nascimento", customer.proprietarioNascimento)
                put("proprietario_sexo", customer.proprietarioSexo)
                if (customer.proprietarioApresentouDoc == null) putNull("proprietario_apresentou_doc") else put("proprietario_apresentou_doc", if (customer.proprietarioApresentouDoc) 1 else 0)
                put("proprietario_qual_doc", customer.proprietarioQual_doc)
                
                put("locatario_nome", customer.locatarioNome)
                put("locatario_cpf", customer.locatarioCpf)
                put("locatario_mae", customer.locatarioMae)
                put("locatario_nascimento", customer.locatarioNascimento)
                put("locatario_sexo", customer.locatarioSexo)
                if (customer.locatarioApresentouDoc == null) putNull("locatario_apresentou_doc") else put("locatario_apresentou_doc", if (customer.locatarioApresentouDoc) 1 else 0)
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
                put("existe_rede_agua", if (customer.existeRedeAgua == null) null else if (customer.existeRedeAgua == true) 1 else 0)
                put("observacao", customer.observacao)
                put("grupo_sugerido", customer.grupoSugerido)
            }
            
            val rowId = db.insert("customers", null, values)
            if (rowId == -1L) {
                throw Exception("Erro de esquema no banco local. Reinstale o app ou limpe os dados.")
            }
            
            try {
                val historyValues = ContentValues().apply {
                    put("matricula", customer.registrationNumber ?: "SEM_MATRICULA")
                    put("qualidade", customer.quality ?: "Ruim")
                    put("date", LocalDate.now().toString())
                }
                db.insertWithOnConflict("history", null, historyValues, SQLiteDatabase.CONFLICT_REPLACE)
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Erro histórico: ${e.message}")
            }
            
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("LocalDatabase", "FALHA SQL: ${e.message}")
            throw e 
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingCustomers(): List<Pair<Int, Customer>> {
        val list = mutableListOf<Pair<Int, Customer>>()
        val db = readableDatabase
        try {
            val cursor = db.query("customers", null, null, null, null, null, "id ASC")
            val cols = mutableMapOf<String, Int>()
            cursor.columnNames.forEach { cols[it] = cursor.getColumnIndex(it) }

            while (cursor.moveToNext()) {
                val localId = cols["id"]?.let { if (it != -1) cursor.getInt(it) else -1 } ?: -1
                val customer = Customer(
                    name = cols["name"]?.let { if (it != -1) cursor.getString(it) else null },
                    registrationNumber = cols["matricula"]?.let { if (it != -1) cursor.getString(it) else null },
                    registrationDigit = cols["digito_matricula"]?.let { if (it != -1) cursor.getString(it) else null },
                    email = cols["email"]?.let { if (it != -1) cursor.getString(it) else null },
                    setor = cols["setor"]?.let { if (it != -1) cursor.getString(it) else null },
                    quadra = cols["quadra"]?.let { if (it != -1) cursor.getString(it) else null },
                    cellPhone = cols["celular"]?.let { if (it != -1) cursor.getString(it) else null },
                    landline = cols["telefone_fixo"]?.let { if (it != -1) cursor.getString(it) else null },
                    
                    isStandardMeasurementBox = cols["caixa_padrao"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    isStandardizedSeals = cols["lacres_padronizados"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    isHdAccessible = cols["hd_acessivel"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    isVacationer = cols["veranista"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    
                    possuiPiscina = cols["possui_piscina"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    possuiCaixaAgua = cols["possui_caixa_agua"]?.let { if (it != -1) cursor.getString(it) else null },
                    beneficiarioSocial = cols["beneficiario_social"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    usaAguaVizinho = cols["usa_agua_vizinho"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    possuiHidrometro = cols["possui_hidrometro"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    
                    latitude = cols["latitude"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getDouble(it) else null },
                    longitude = cols["longitude"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getDouble(it) else null },
                    locationStatus = cols["situacao_local"]?.let { if (it != -1) cursor.getString(it) else null },
                    economiesCount = cols["qtd_economias"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) else null },
                    
                    createdAt = cols["criado_em"]?.let { if (it != -1) cursor.getString(it) else null },
                    capturedAt = cols["capturado_em"]?.let { if (it != -1) cursor.getString(it) else null },
                    addedBy = cols["adicionado_por"]?.let { if (it != -1) cursor.getString(it) else null },
                    cidadeId = cols["cidade_id"]?.let { if (it != -1) cursor.getString(it) else null },
                    leituristaId = cols["leiturista_id"]?.let { if (it != -1) cursor.getString(it) else null },
                    date = cols["date"]?.let { if (it != -1) cursor.getString(it) else null },
                    quality = cols["qualidade"]?.let { if (it != -1) cursor.getString(it) else "Ruim" } ?: "Ruim",
                    
                    entrevistadoNome = cols["entrevistado_nome"]?.let { if (it != -1) cursor.getString(it) else null },
                    entrevistadoCpf = cols["entrevistado_cpf"]?.let { if (it != -1) cursor.getString(it) else null },
                    entrevistadoMae = cols["entrevistado_mae"]?.let { if (it != -1) cursor.getString(it) else null },
                    entrevistadoNascimento = cols["entrevistado_nascimento"]?.let { if (it != -1) cursor.getString(it) else null },
                    entrevistadoSexo = cols["entrevistado_sexo"]?.let { if (it != -1) cursor.getString(it) else null },
                    entrevistadoApresentouDoc = cols["entrevistado_apresentou_doc"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    entrevistadoQualDoc = cols["entrevistado_qual_doc"]?.let { if (it != -1) cursor.getString(it) else null },
                    
                    proprietarioNome = cols["proprietario_nome"]?.let { if (it != -1) cursor.getString(it) else null },
                    proprietarioCpf = cols["proprietario_cpf"]?.let { if (it != -1) cursor.getString(it) else null },
                    proprietarioMae = cols["proprietario_mae"]?.let { if (it != -1) cursor.getString(it) else null },
                    proprietarioNascimento = cols["proprietario_nascimento"]?.let { if (it != -1) cursor.getString(it) else null },
                    proprietarioSexo = cols["proprietario_sexo"]?.let { if (it != -1) cursor.getString(it) else null },
                    proprietarioApresentouDoc = cols["proprietario_apresentou_doc"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    proprietarioQual_doc = cols["proprietario_qual_doc"]?.let { if (it != -1) cursor.getString(it) else null },
                    
                    locatarioNome = cols["locatario_nome"]?.let { if (it != -1) cursor.getString(it) else null },
                    locatarioCpf = cols["locatario_cpf"]?.let { if (it != -1) cursor.getString(it) else null },
                    locatarioMae = cols["locatario_mae"]?.let { if (it != -1) cursor.getString(it) else null },
                    locatarioNascimento = cols["locatario_nascimento"]?.let { if (it != -1) cursor.getString(it) else null },
                    locatarioSexo = cols["locatario_sexo"]?.let { if (it != -1) cursor.getString(it) else null },
                    locatarioApresentouDoc = cols["locatario_apresentou_doc"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    locatarioQualDoc = cols["locatario_qual_doc"]?.let { if (it != -1) cursor.getString(it) else null },
                    
                    logradouro = cols["logradouro"]?.let { if (it != -1) cursor.getString(it) else null },
                    numero = cols["numero"]?.let { if (it != -1) cursor.getString(it) else null },
                    complemento = cols["complemento"]?.let { if (it != -1) cursor.getString(it) else null },
                    bairro = cols["bairro"]?.let { if (it != -1) cursor.getString(it) else null },
                    uf = cols["uf"]?.let { if (it != -1) cursor.getString(it) else null },
                    cep = cols["cep"]?.let { if (it != -1) cursor.getString(it) else null },
                    cidade = cols["cidade"]?.let { if (it != -1) cursor.getString(it) else null },
                    
                    pavimentoRua = cols["pavimento_rua"]?.let { if (it != -1) cursor.getString(it) else null },
                    pavimentoCalcada = cols["pavimento_calcada"]?.let { if (it != -1) cursor.getString(it) else null },
                    hidrometroProximo = cols["hidrometro_proximo"]?.let { if (it != -1) cursor.getString(it) else null },
                    fonteAbastecimento = cols["fonte_abastecimento"]?.let { if (it != -1) cursor.getString(it) else null },
                    existeRedeAgua = cols["existe_rede_agua"]?.let { if (it != -1 && !cursor.isNull(it)) cursor.getInt(it) == 1 else null },
                    observacao = cols["observacao"]?.let { if (it != -1) cursor.getString(it) else null },
                    grupoSugerido = cols["grupo_sugerido"]?.let { if (it != -1) cursor.getString(it) else null },
                    isSynced = false
                )
                list.add(localId to customer)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Erro leitura: ${e.message}")
        }
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

    companion object {
        private const val DATABASE_NAME = "sanitation_local_v13.db"
        private const val DATABASE_VERSION = 20

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
                grupo_sugerido TEXT
            )
        """

        private const val CREATE_TABLE_STATS = """
            CREATE TABLE stats (
                stat_key TEXT PRIMARY KEY,
                record_value INTEGER,
                last_updated TEXT
            )
        """

        private const val CREATE_TABLE_HISTORY = """
            CREATE TABLE history (
                matricula TEXT PRIMARY KEY,
                qualidade TEXT,
                date TEXT
            )
        """
    }
}
