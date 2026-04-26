package com.example.oaplicativo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.oaplicativo.model.Customer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CUSTOMERS)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_sync_status ON customers(sync_status);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_date ON customers(date);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Reset para desenvolvimento para incluir a nova coluna 'date'
        db.execSQL("DROP TABLE IF EXISTS customers")
        onCreate(db)
    }

    fun purgeOldRecords() {
        try {
            val db = writableDatabase
            val startOfToday = ZonedDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val cutoffStr = startOfToday.format(formatter)

            db.delete("customers", "created_at < ?", arrayOf(cutoffStr))
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Erro na faxina diária: ${e.message}")
        }
    }

    fun saveCustomerOffline(customer: Customer) {
        try {
            val db = writableDatabase
            
            customer.registrationNumber?.let { regNum ->
                db.delete("customers", "registration_number = ? AND sync_status = 0", arrayOf(regNum))
            }

            val values = ContentValues().apply {
                put("name", customer.name)
                put("registration_number", customer.registrationNumber)
                put("registration_digit", customer.registrationDigit)
                put("email", customer.email)
                put("landline", customer.landline)
                put("cell_phone", customer.cellPhone)
                put("is_standard_measurement_box", if (customer.isStandardMeasurementBox == true) 1 else 0)
                put("is_standardized_seals", if (customer.isStandardizedSeals == true) 1 else 0)
                put("is_hd_accessible", if (customer.isHdAccessible == true) 1 else 0)
                put("is_vacationer", if (customer.isVacationer == true) 1 else 0)
                put("latitude", customer.latitude)
                put("longitude", customer.longitude)
                put("location_status", customer.locationStatus)
                put("economies_count", customer.economiesCount)
                put("created_at", customer.createdAt ?: ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                
                // Auditoria
                put("added_by", customer.addedBy)
                put("captured_at", customer.capturedAt)
                put("synced_at", customer.syncedAt)
                put("date", customer.date)
                
                put("sync_status", 0) 
            }
            db.insertWithOnConflict("customers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Erro crítico ao salvar localmente: ${e.message}")
            throw e
        }
    }

    fun getPendingCustomers(): List<Pair<Int, Customer>> {
        val list = mutableListOf<Pair<Int, Customer>>()
        val db = readableDatabase
        val cursor = db.query("customers", null, "sync_status = 0", null, null, null, "id ASC")
        
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(c.getColumnIndexOrThrow("id"))
                val customer = Customer(
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    registrationNumber = c.getString(c.getColumnIndexOrThrow("registration_number")),
                    registrationDigit = c.getString(c.getColumnIndexOrThrow("registration_digit")),
                    email = c.getString(c.getColumnIndexOrThrow("email")),
                    landline = c.getString(c.getColumnIndexOrThrow("landline")),
                    cellPhone = c.getString(c.getColumnIndexOrThrow("cell_phone")),
                    isStandardMeasurementBox = c.getInt(c.getColumnIndexOrThrow("is_standard_measurement_box")) == 1,
                    isStandardizedSeals = c.getInt(c.getColumnIndexOrThrow("is_standardized_seals")) == 1,
                    isHdAccessible = c.getInt(c.getColumnIndexOrThrow("is_hd_accessible")) == 1,
                    isVacationer = c.getInt(c.getColumnIndexOrThrow("is_vacationer")) == 1,
                    latitude = if (c.isNull(c.getColumnIndexOrThrow("latitude"))) null else c.getDouble(c.getColumnIndexOrThrow("latitude")),
                    longitude = if (c.isNull(c.getColumnIndexOrThrow("longitude"))) null else c.getDouble(c.getColumnIndexOrThrow("longitude")),
                    locationStatus = c.getString(c.getColumnIndexOrThrow("location_status")),
                    economiesCount = c.getInt(c.getColumnIndexOrThrow("economies_count")),
                    createdAt = c.getString(c.getColumnIndexOrThrow("created_at")),
                    addedBy = c.getString(c.getColumnIndexOrThrow("added_by")),
                    capturedAt = c.getString(c.getColumnIndexOrThrow("captured_at")),
                    syncedAt = c.getString(c.getColumnIndexOrThrow("synced_at")),
                    date = c.getString(c.getColumnIndexOrThrow("date"))
                )
                list.add(id to customer)
            }
        }
        return list
    }

    fun deleteSyncedCustomer(localId: Int) {
        try {
            val db = writableDatabase
            db.delete("customers", "id = ?", arrayOf(localId.toString()))
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Erro ao deletar item sincronizado: ${e.message}")
        }
    }

    companion object {
        private const val DATABASE_NAME = "leiturista_local.db"
        private const val DATABASE_VERSION = 5 // Aumentado para coluna DATE
        private const val CREATE_TABLE_CUSTOMERS = """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                registration_number TEXT,
                registration_digit TEXT,
                email TEXT,
                landline TEXT,
                cell_phone TEXT,
                is_standard_measurement_box INTEGER,
                is_standardized_seals INTEGER,
                is_hd_accessible INTEGER,
                is_vacationer INTEGER,
                latitude REAL,
                longitude REAL,
                location_status TEXT,
                economies_count INTEGER,
                created_at TEXT,
                added_by TEXT,
                captured_at TEXT,
                synced_at TEXT,
                date TEXT,
                sync_status INTEGER DEFAULT 0
            )
        """
    }
}