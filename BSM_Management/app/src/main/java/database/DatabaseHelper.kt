package database

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bsm_management.ui.room.UiRoom
import com.example.bsm_management.ui.tenant.Tenant

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ============================================================
    // CREATE SCHEMA
    // ============================================================
    override fun onCreate(db: SQLiteDatabase) {

        // ===== USERS =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            phone TEXT NOT NULL UNIQUE,
            name TEXT NOT NULL,
            password TEXT NOT NULL
        );
    """.trimIndent())

        // ===== ROOMS =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS rooms (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            floor INTEGER NOT NULL DEFAULT 1,
            status TEXT NOT NULL DEFAULT 'EMPTY',
            baseRent INTEGER NOT NULL DEFAULT 0,
            maxPeople INTEGER NOT NULL DEFAULT 0,
            tenantPhone TEXT DEFAULT NULL
        );
    """.trimIndent())

        // ===== CONTRACTS =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS contracts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roomId INTEGER NOT NULL,
            tenantName TEXT NOT NULL,
            tenantPhone TEXT NOT NULL,
            startDate INTEGER NOT NULL,
            endDate INTEGER,
            deposit INTEGER NOT NULL DEFAULT 0,
            active INTEGER NOT NULL DEFAULT 1,
            FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE
        );
    """.trimIndent())

        // ===== INVOICES =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS invoices (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roomId INTEGER NOT NULL,
            periodYear INTEGER NOT NULL,
            periodMonth INTEGER NOT NULL,
            roomRent INTEGER NOT NULL,
            electricKwh INTEGER NOT NULL DEFAULT 0,
            waterM3 INTEGER NOT NULL DEFAULT 0,
            serviceFee INTEGER NOT NULL DEFAULT 0,
            totalAmount INTEGER NOT NULL,
            paid INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL,
            dueAt INTEGER,
            reason TEXT,
            FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
            UNIQUE(roomId, periodYear, periodMonth)
        );
    """.trimIndent())

        // ===== MESSAGES =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            hostelName TEXT,
            sender TEXT,
            message TEXT,
            time TEXT,
            isRead INTEGER NOT NULL DEFAULT 0
        );
    """.trimIndent())

        // ===== TENANTS =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS tenants (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roomId INTEGER,
            name TEXT NOT NULL,
            phone TEXT NOT NULL,
            isUsingApp INTEGER NOT NULL DEFAULT 0,
            hasTempReg INTEGER NOT NULL DEFAULT 0,
            hasPaper INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL,
            slotIndex INTEGER NOT NULL DEFAULT 1,
            isOld INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE SET NULL
        );
    """.trimIndent())

        // ===== ROOM SERVICES (PER ROOM) =====
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS services (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roomId INTEGER NOT NULL,
            serviceName TEXT NOT NULL,
            enabled INTEGER NOT NULL DEFAULT 1,
            price INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
        );
    """.trimIndent())

        // ===== INDEXES =====
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_isRead ON messages(isRead);")

    }



    // ============================================================
    // ON UPGRADE
    // ============================================================
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("ALTER TABLE rooms ADD COLUMN maxPeople INTEGER NOT NULL DEFAULT 0;")
        } catch (_: Exception) {}

        try {
            db.execSQL("ALTER TABLE services ADD COLUMN price INTEGER DEFAULT 0;")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE rooms ADD COLUMN tenantPhone TEXT")
        } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE tenants ADD COLUMN slotIndex INTEGER NOT NULL DEFAULT 1") } catch(_:Exception){}
        try { db.execSQL("ALTER TABLE tenants ADD COLUMN isOld INTEGER NOT NULL DEFAULT 0") } catch(_:Exception){}
        try {
            db.execSQL("""
        CREATE TABLE IF NOT EXISTS services (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roomId INTEGER NOT NULL,
            serviceName TEXT NOT NULL,
            enabled INTEGER NOT NULL DEFAULT 1,
            price INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE
        );
    """.trimIndent())
        } catch(_: Exception) {}

        db.setVersion(newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    // ============================================================
    // MESSAGE CRUD
    // ============================================================
    data class Message(
        val id: Int,
        val hostelName: String?,
        val sender: String,
        val message: String,
        val time: String,
        val isRead: Boolean
    )

    fun insertMessage(sender: String, message: String, time: String, isRead: Boolean = false, hostelName: String? = "Nhà trọ Duy") {
        val cv = ContentValues().apply {
            put("hostelName", hostelName)
            put("sender", sender)
            put("message", message)
            put("time", time)
            put("isRead", if (isRead) 1 else 0)
        }
        writableDatabase.insert("messages", null, cv)
    }

    fun getAllMessages(): List<Message> {
        val list = mutableListOf<Message>()
        val c = readableDatabase.rawQuery("SELECT * FROM messages ORDER BY id DESC", null)
        c.use {
            while (it.moveToNext()) {
                list.add(
                    Message(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        hostelName = it.getString(it.getColumnIndexOrThrow("hostelName")),
                        sender = it.getString(it.getColumnIndexOrThrow("sender")),
                        message = it.getString(it.getColumnIndexOrThrow("message")),
                        time = it.getString(it.getColumnIndexOrThrow("time")),
                        isRead = it.getInt(it.getColumnIndexOrThrow("isRead")) == 1
                    )
                )
            }
        }
        return list
    }

    fun deleteMessage(msg: Message): Int {
        return writableDatabase.delete("messages", "id=?", arrayOf(msg.id.toString()))
    }

    // ============================================================
// ROOM-SERVICES CRUD (NEW)
// ============================================================

    // Tạo default service cho 1 phòng
    fun createDefaultServicesForRoom(roomId: Long) {
        val defaultServices = listOf(
            Triple("Dịch vụ điện", true, 3500),
            Triple("Dịch vụ nước", true, 12000),
            Triple("Dịch vụ rác", true, 20000),
            Triple("Dịch vụ internet/mạng", true, 100000)
        )

        val db = writableDatabase
        defaultServices.forEach { (name, enabled, price) ->
            val cv = ContentValues().apply {
                put("roomId", roomId)
                put("serviceName", name)
                put("enabled", if (enabled) 1 else 0)
                put("price", price)
            }
            db.insert("services", null, cv)
        }
    }


    // Lấy dịch vụ theo phòng
    fun getServicesForRoom(roomId: Long): List<Triple<String, Boolean, Int>> {

        val list = mutableListOf<Triple<String, Boolean, Int>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT serviceName, enabled, price
            FROM services
            WHERE roomId = ?
        """.trimIndent(),
            arrayOf(roomId.toString())
        )

        c.use {
            while (it.moveToNext()) {
                list += Triple(
                    it.getString(0),
                    it.getInt(1) == 1,
                    it.getInt(2)
                )
            }
        }
        return list
    }


    // Update theo từng phòng
    fun updateRoomService(
        roomId: Long, serviceName: String,
        enabled: Boolean? = null, price: Int? = null) {
        val cv = ContentValues()

        enabled?.let { cv.put("enabled", if (it) 1 else 0) }
        price?.let { cv.put("price", it) }

        writableDatabase.update(
            "services",
            cv,
            "roomId=? AND serviceName=?",
            arrayOf(roomId.toString(), serviceName)
        )
    }

    // ============================================================
    // ROOM HELPERS
    // ============================================================
    fun hasAnyRoom(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM rooms LIMIT 1", null).use { c ->
            return c.moveToFirst()
        }
    }

    fun countRooms(): Long =
        DatabaseUtils.queryNumEntries(readableDatabase, "rooms")

    fun deleteAllRoomsCascade(): Int {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val current = countRooms().toInt()
            db.delete("rooms", null, null)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('rooms','contracts','invoices')")
            db.setTransactionSuccessful()
            current
        } finally {
            db.endTransaction()
        }
    }

    // ============================================================
// TENANT CRUD
// ============================================================

    fun getAllRooms(): List<UiRoom> {
        val list = ArrayList<UiRoom>()
        val sql = """
        SELECT  
            id,
            name,
            baseRent,
            floor,
            status,
            maxPeople,
            tenantPhone
        FROM rooms
        ORDER BY id ASC
    """
        val c = readableDatabase.rawQuery(sql, null)

        c.use {
            while (it.moveToNext()) {

                val id = it.getLong(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val baseRent = it.getInt(it.getColumnIndexOrThrow("baseRent"))
                val floor = it.getInt(it.getColumnIndexOrThrow("floor"))
                val status = it.getString(it.getColumnIndexOrThrow("status"))
                val maxPeople = it.getInt(it.getColumnIndexOrThrow("maxPeople"))
                val phone = it.getString(it.getColumnIndexOrThrow("tenantPhone"))

                // Lấy số người hiện tại từ table tenants
                val tenantCount = DatabaseUtils.longForQuery(
                    readableDatabase,
                    "SELECT COUNT(*) FROM tenants WHERE roomId = $id",
                    null
                ).toInt()

                list.add(
                    UiRoom(
                        id = id,
                        name = name,
                        baseRent = baseRent,
                        floor = floor,
                        status = status,
                        tenantCount = tenantCount,
                        maxPeople = maxPeople,
                        contractEnd = null,          // chưa dùng → để null
                        appUsed = null,              // chưa dùng → null
                        onlineSigned = null,         // chưa dùng → null
                        phone = phone
                    )
                )
            }
        }

        return list
    }
    fun addTenant(
        roomId: Int,
        name: String,
        phone: String,
        slotIndex: Int
    ) {
        val cv = ContentValues().apply {
            put("roomId", roomId)
            put("name", name)
            put("phone", phone)
            put("slotIndex", slotIndex)
            put("createdAt", System.currentTimeMillis())
            put("isOld", 0)
        }
        writableDatabase.insert("tenants", null, cv)
    }

    fun getTenantSlots(roomId: Int, maxPeople: Int): List<Tenant?> {
        val slots = MutableList<Tenant?>(maxPeople) { null }

        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, name, phone, slotIndex, isUsingApp, hasTempReg, hasPaper
            FROM tenants
            WHERE roomId = ? AND isOld = 0
            ORDER BY slotIndex ASC
        """,
            arrayOf(roomId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                val index = it.getInt(3) - 1
                if (index !in 0 until maxPeople) continue

                slots[index] = Tenant(
                    id = it.getInt(0),
                    name = it.getString(1),
                    phone = it.getString(2),
                    roomId = roomId,
                    isUsingApp = it.getInt(4) == 1,
                    hasTemporaryResidence = it.getInt(5) == 1,
                    hasEnoughDocuments = it.getInt(6) == 1,
                    slotIndex = index + 1
                )
            }
        }

        return slots
    }



    fun updateTenant(t: com.example.bsm_management.ui.tenant.Tenant) {
        val cv = ContentValues().apply {
            put("name", t.name)
            put("phone", t.phone)
            put("isUsingApp", if (t.isUsingApp) 1 else 0)
        }
        writableDatabase.update("tenants", cv, "id=?", arrayOf(t.id.toString()))
    }

    fun moveTenantToOld(id: Int) {
        val cv = ContentValues().apply {
            put("isOld", 1)
            putNull("roomId")
        }
        writableDatabase.update("tenants", cv, "id=?", arrayOf(id.toString()))
    }
    fun moveRoomTenantsToOld(roomId: Int) {
        writableDatabase.execSQL(
            "UPDATE tenants SET roomId = NULL, isOld = 1 WHERE roomId = $roomId"
        )
    }


    fun updateTenantUsingApp(id: Int, isUsing: Boolean) {
        val cv = ContentValues().apply {
            put("isUsingApp", if (isUsing) 1 else 0)
        }
        writableDatabase.update("tenants", cv, "id=?", arrayOf(id.toString()))
    }
    fun getAllTenantsActive(): List<Tenant> {
        val list = mutableListOf<Tenant>()

        val c = readableDatabase.rawQuery(
            """
            SELECT id, name, phone, roomId, isUsingApp, hasTempReg, hasPaper
            FROM tenants
            WHERE isOld = 0
            ORDER BY id DESC
        """, null
        )

        c.use {
            while (it.moveToNext()) {
                list += Tenant(
                    id = it.getInt(0),
                    name = it.getString(1),
                    phone = it.getString(2),
                    roomId = it.getInt(3),
                    isUsingApp = it.getInt(4) == 1,
                    hasTemporaryResidence = it.getInt(5) == 1,
                    hasEnoughDocuments = it.getInt(6) == 1,
                    slotIndex = -1
                )
            }
        }

        return list
    }
    fun getOldTenants(): List<Tenant> {
        val list = mutableListOf<Tenant>()

        val c = readableDatabase.rawQuery(
            """
            SELECT id, name, phone, isUsingApp, hasTempReg, hasPaper
            FROM tenants
            WHERE isOld = 1
            ORDER BY id DESC
        """, null
        )

        c.use {
            while (it.moveToNext()) {
                list += Tenant(
                    id = it.getInt(0),
                    name = it.getString(1),
                    phone = it.getString(2),
                    roomId = null, // khách cũ không thuộc phòng nào
                    isUsingApp = it.getInt(3) == 1,
                    hasTemporaryResidence = it.getInt(4) == 1,
                    hasEnoughDocuments = it.getInt(5) == 1,
                    slotIndex = -1
                )
            }
        }

        return list
    }
    fun countRoomsByStatus(status: String): Int {
        val sql = "SELECT COUNT(*) FROM rooms WHERE status = ?"
        return DatabaseUtils.longForQuery(readableDatabase, sql, arrayOf(status)).toInt()
    }
    fun countRentingRooms(): Int {
        return countRoomsByStatus("RENTED")
    }
    fun countAvailableRooms(): Int {
        return countRoomsByStatus("EMPTY")
    }
    fun countContractsEndingSoon(days: Int = 7): Int {
        val now = System.currentTimeMillis()
        val limit = now + days * 24 * 60 * 60 * 1000L

        val sql = """
        SELECT COUNT(*)
        FROM contracts
        WHERE active = 1
        AND endDate IS NOT NULL
        AND endDate BETWEEN ? AND ?
    """
        return DatabaseUtils.longForQuery(
            readableDatabase,
            sql,
            arrayOf(now.toString(), limit.toString())
        ).toInt()
    }
    fun countContractsOverdue(): Int {
        val now = System.currentTimeMillis()
        val sql = """
        SELECT COUNT(*)
        FROM contracts
        WHERE active = 1
        AND endDate IS NOT NULL
        AND endDate < ?
    """
        return DatabaseUtils.longForQuery(
            readableDatabase,
            sql,
            arrayOf(now.toString())
        ).toInt()
    }
    fun countContractsNotified(): Int {
        val sql = "SELECT COUNT(*) FROM contracts WHERE active = 0"
        return DatabaseUtils.longForQuery(readableDatabase, sql, null).toInt()
    }



    companion object {
        const val DB_NAME = "bsm.db"
        private const val DB_VERSION = 3
    }
}
