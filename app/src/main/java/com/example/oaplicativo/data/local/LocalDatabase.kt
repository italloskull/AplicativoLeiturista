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
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS customers")
            db.execSQL("DROP TABLE IF EXISTS stats")
            onCreate(db)
        }
    }

    fun purgeOldRecords() {
        val db = writableDatabase
        db.delete("customers", null, null)
        db.close()
    }

    // --- LÓGICA DE GAMIFICAÇÃO ---

    fun updateRecordIfHigher(currentCount: Int) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT record_value FROM stats WHERE stat_key = 'daily_record'", null)
        var highest = 0
        if (cursor.moveToFirst()) {
            highest = cursor.getInt(0)
        }
        cursor.close()

        if (currentCount > highest) {
            val values = ContentValues().apply {
                put("stat_key", "daily_record")
                put("record_value", currentCount)
                put("last_updated", LocalDate.now().toString())
            }
            db.insertWithOnConflict("stats", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
        db.close()
    }

    fun getPersonalRecord(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT record_value FROM stats WHERE stat_key = 'daily_record'", null)
        val record = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return record
    }

    fun getTodayStats(): Map<String, Int> {
        val db = readableDatabase
        val stats = mutableMapOf("Boa" to 0, "Regular" to 0, "Ruim" to 0, "Total" to 0)
        val cursor = db.rawQuery("SELECT quality, COUNT(*) FROM customers GROUP BY quality", null)
        while (cursor.moveToNext()) {
            val q = cursor.getString(0) ?: "Ruim"
            val count = cursor.getInt(1)
            stats[q] = count
            stats["Total"] = (stats["Total"] ?: 0) + count
        }
        cursor.close()
        return stats
    }

    // --- LÓGICA DE SINCRONIZAÇÃO (RESTAURADA) ---

    fun saveCustomerOffline(customer: Customer) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", customer.name)
            put("registration_number", customer.registrationNumber)
            put("registration_digit", customer.registrationDigit)
            put("email", customer.email)
            put("cell_phone", customer.cellPhone)
            put("latitude", customer.latitude)
            put("longitude", customer.longitude)
            put("quality", customer.quality)
            put("created_at", customer.createdAt)
            put("date", customer.date)
        }
        db.insert("customers", null, values)
        db.close()
    }

    fun getPendingCustomers(): List<Pair<Int, Customer>> {
        val db = readableDatabase
        val list = mutableListOf<Pair<Int, Customer>>()
        val cursor = db.query("customers", null, null, null, null, null, "id ASC")
        
        while (cursor.moveToNext()) {
            val localId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val customer = Customer(
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                registrationNumber = cursor.getString(cursor.getColumnIndexOrThrow("registration_number")),
                registrationDigit = cursor.getString(cursor.getColumnIndexOrThrow("registration_digit")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cellPhone = cursor.getString(cursor.getColumnIndexOrThrow("cell_phone")),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                quality = cursor.getString(cursor.getColumnIndexOrThrow("quality")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                isSynced = false
            )
            list.add(localId to customer)
        }
        cursor.close()
        return list
    }

    fun deleteSyncedCustomer(localId: Int) {
        val db = writableDatabase
        db.delete("customers", "id = ?", arrayOf(localId.toString()))
        db.close()
    }

    companion object {
        private const val DATABASE_NAME = "sanitation_local.db"
        private const val DATABASE_VERSION = 6

        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                registration_number TEXT,
                registration_digit TEXT,
                email TEXT,
                cell_phone TEXT,
                latitude REAL,
                longitude REAL,
                quality TEXT,
                created_at TEXT,
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
