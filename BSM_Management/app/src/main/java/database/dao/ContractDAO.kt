package database.dao

import android.content.ContentValues
import android.content.Context
import com.example.bsm_management.ui.contract.Contract
import database.DatabaseHelper

class ContractDAO(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insert(contract: Contract): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("roomId", contract.roomId)
            put("tenantName", contract.tenantName)
            put("tenantPhone", contract.tenantPhone)
            put("startDate", contract.startDate)
            put("endDate", contract.endDate)
            put("deposit", contract.deposit)
            put("active", contract.active)
        }
        return db.insert("contracts", null, values)
    }

    fun update(contract: Contract): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("tenantName", contract.tenantName)
            put("tenantPhone", contract.tenantPhone)
            put("startDate", contract.startDate)
            put("endDate", contract.endDate)
            put("deposit", contract.deposit)
            put("active", contract.active)
        }
        return db.update("contracts", values, "id=?", arrayOf(contract.id.toString()))
    }

    fun delete(id: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete("contracts", "id=?", arrayOf(id.toString()))
    }

    fun getAll(): List<Contract> {
        val list = mutableListOf<Contract>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM contracts", null)
        while (cursor.moveToNext()) {
            list.add(
                Contract(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    roomId = cursor.getInt(cursor.getColumnIndexOrThrow("roomId")),
                    tenantName = cursor.getString(cursor.getColumnIndexOrThrow("tenantName")),
                    tenantPhone = cursor.getString(cursor.getColumnIndexOrThrow("tenantPhone")),
                    startDate = cursor.getLong(cursor.getColumnIndexOrThrow("startDate")),
                    endDate = if (cursor.isNull(cursor.getColumnIndexOrThrow("endDate"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("endDate")),
                    deposit = cursor.getInt(cursor.getColumnIndexOrThrow("deposit")),
                    active = cursor.getInt(cursor.getColumnIndexOrThrow("active"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getById(id: Int): Contract? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM contracts WHERE id=?", arrayOf(id.toString()))
        var contract: Contract? = null
        if (cursor.moveToFirst()) {
            contract = Contract(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                roomId = cursor.getInt(cursor.getColumnIndexOrThrow("roomId")),
                tenantName = cursor.getString(cursor.getColumnIndexOrThrow("tenantName")),
                tenantPhone = cursor.getString(cursor.getColumnIndexOrThrow("tenantPhone")),
                startDate = cursor.getLong(cursor.getColumnIndexOrThrow("startDate")),
                endDate = if (cursor.isNull(cursor.getColumnIndexOrThrow("endDate"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("endDate")),
                deposit = cursor.getInt(cursor.getColumnIndexOrThrow("deposit")),
                active = cursor.getInt(cursor.getColumnIndexOrThrow("active"))
            )
        }
        cursor.close()
        return contract
    }
}
