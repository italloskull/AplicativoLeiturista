package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.oaplicativo.model.Customer
import java.time.LocalDate

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL(CREATE_TABLE_STATS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Reset para v11: Garante persistência de coordenadas geográficas
        db.execSQL("DROP TABLE IF EXISTS customers")
        db.execSQL("DROP TABLE IF EXISTS stats")
        onCreate(db)
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
        try {
            val cursor = readableDatabase.rawQuery("SELECT qualidade, COUNT(*) FROM customers GROUP BY qualidade", null)
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
        val values = ContentValues().apply {
            put("name", customer.name)
            put("matricula", customer.registrationNumber)
            put("qualidade", customer.quality)
            put("criado_em", customer.createdAt)
            put("capturado_em", customer.capturedAt)
            put("adicionado_por", customer.addedBy)
            put("cidade_id", customer.cidadeId)
            
            // --- CORE FIX: Salvando as coordenadas no SQLite ---
            put("latitude", customer.latitude)
            put("longitude", customer.longitude)
            
            put("entrevistado_nome", customer.entrevistadoNome)
            put("entrevistado_cpf", customer.entrevistadoCpf)
            put("proprietario_nome", customer.proprietarioNome)
            put("locatario_nome", customer.locatarioNome)
            put("celular", customer.cellPhone)
            put("telefone_fixo", customer.landline)
            put("pavimento_calcada", customer.pavimentoCalcada)
            put("cidade", customer.cidade)
            put("date", customer.date)
        }
        writableDatabase.insert("customers", null, values)
    }

    fun getPendingCustomers(): List<Pair<Int, Customer>> {
        val list = mutableListOf<Pair<Int, Customer>>()
        try {
            val cursor = readableDatabase.query("customers", null, null, null, null, null, "id ASC")
            while (cursor.moveToNext()) {
                val localId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val customer = Customer(
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                    quality = cursor.getString(cursor.getColumnIndexOrThrow("qualidade")),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("criado_em")),
                    capturedAt = cursor.getString(cursor.getColumnIndexOrThrow("capturado_em")),
                    addedBy = cursor.getString(cursor.getColumnIndexOrThrow("adicionado_por")),
                    cidadeId = cursor.getString(cursor.getColumnIndexOrThrow("cidade_id")),
                    
                    // --- CORE FIX: Recuperando as coordenadas do SQLite ---
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    
                    entrevistadoNome = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_nome")),
                    entrevistadoCpf = cursor.getString(cursor.getColumnIndexOrThrow("entrevistado_cpf")),
                    proprietarioNome = cursor.getString(cursor.getColumnIndexOrThrow("proprietario_nome")),
                    locatarioNome = cursor.getString(cursor.getColumnIndexOrThrow("locatario_nome")),
                    cellPhone = cursor.getString(cursor.getColumnIndexOrThrow("celular")),
                    landline = cursor.getString(cursor.getColumnIndexOrThrow("telefone_fixo")),
                    pavimentoCalcada = cursor.getString(cursor.getColumnIndexOrThrow("pavimento_calcada")),
                    cidade = cursor.getString(cursor.getColumnIndexOrThrow("cidade")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    isSynced = false
                )
                list.add(localId to customer)
            }
            cursor.close()
        } catch (_: Exception) { }
        return list
    }

    fun deleteSyncedCustomer(localId: Int) {
        writableDatabase.delete("customers", "id = ?", arrayOf(localId.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "sanitation_local_v11.db"
        private const val DATABASE_VERSION = 11 

        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                matricula TEXT,
                qualidade TEXT,
                criado_em TEXT,
                capturado_em TEXT,
                adicionado_por TEXT,
                cidade_id TEXT,
                latitude REAL,
                longitude REAL,
                entrevistado_nome TEXT,
                entrevistado_cpf TEXT,
                proprietario_nome TEXT,
                locatario_nome TEXT,
                celular TEXT,
                telefone_fixo TEXT,
                pavimento_calcada TEXT,
                cidade TEXT,
                date TEXT
            )
        """

        private const val CREATE_TABLE_STATS = """
            CREATE TABLE stats (
                stat_key TEXT PRIMARY KEY,
                record_value INTEGER,
                last_updated TEXT
            )
        """
    }
}
