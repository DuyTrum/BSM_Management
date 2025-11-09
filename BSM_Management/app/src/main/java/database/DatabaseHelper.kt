package database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.DatabaseUtils

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    // ============================================================
    // CONFIG
    // ============================================================
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
                baseRent INTEGER NOT NULL DEFAULT 0
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

        // ===== MESSAGES (INBOX) =====
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

        // ===== SEED DATA =====
        db.execSQL("""
            INSERT INTO users (phone, name, password) VALUES
            ('12345678','Admin','123456'),
            ('22222222','Nguyen Van A','123456'),
            ('33333333','Le Thi C','123456');
        """)

        db.execSQL("""
            INSERT INTO rooms (name, status, baseRent) VALUES
            ('P101', 'RENTED', 1500000),
            ('P102', 'RENTED', 1500000),
            ('P201', 'EMPTY', 1800000),
            ('P202', 'RENTED', 1800000),
            ('P203', 'EMPTY', 1800000);
        """)

        db.execSQL("""
            INSERT INTO contracts (roomId, tenantName, tenantPhone, startDate, endDate, deposit, active) VALUES
            (1, 'Le Van C',  '0901111111', strftime('%s','now','-5 months')*1000, NULL, 2000000, 1),
            (2, 'Tran Thi B','0902222222', strftime('%s','now','-2 months')*1000, NULL, 2000000, 1),
            (4, 'Pham D',    '0903333333', strftime('%s','now','-1 months')*1000, NULL, 2500000, 1);
        """)

        // Tin nhắn mẫu
        db.execSQL("""
            INSERT INTO messages (hostelName, sender, message, time, isRead) VALUES
            ('Nhà trọ Duy', 'Chủ trọ', 'Xin chào, đây là tin nhắn mẫu đầu tiên', '08:45 01/11/2025', 0),
            ('Nhà trọ Duy', 'Hệ thống', 'Hóa đơn tháng 10 đã được tạo', '09:30 02/11/2025', 1);
        """)
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS services (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            price float DEFAULT 0,
            enabled INTEGER NOT NULL DEFAULT 0
        );
    """.trimIndent())

        // Seed mặc định
        db.execSQL("""
        INSERT INTO services (name, price, enabled) VALUES
        ('Dịch vụ điện', 25000, 1),
        ('Dịch vụ nước', 25000, 1),
        ('Dịch vụ rác', 50000, 0),
        ('Dịch vụ internet/mạng', 250000, 0);
    """)

        // ===== INDEXES =====
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_isRead ON messages(isRead);")
    }

    // ============================================================
    // ON UPGRADE / DOWNGRADE
    // ============================================================
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.beginTransaction()
        try {
            db.execSQL("DROP TABLE IF EXISTS users")
            db.execSQL("DROP TABLE IF EXISTS rooms")
            db.execSQL("DROP TABLE IF EXISTS contracts")
            db.execSQL("DROP TABLE IF EXISTS invoices")
            db.execSQL("DROP TABLE IF EXISTS messages")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    // ============================================================
    // ✅ MESSAGE CRUD (Inbox)
    // ============================================================
    data class Message(
        val id: Int,
        val hostelName: String?,
        val sender: String,
        val message: String,
        val time: String,
        val isRead: Boolean
    )

    fun insertMessage(
        sender: String,
        message: String,
        time: String,
        isRead: Boolean = false,
        hostelName: String? = "Nhà trọ Duy"
    ) {
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
    fun updateService(name: String, enabled: Boolean) {
        val cv = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        writableDatabase.update("services", cv, "name=?", arrayOf(name))
    }

    fun getAllServices(): List<Pair<String, Boolean>> {
        val result = mutableListOf<Pair<String, Boolean>>()
        val c = readableDatabase.rawQuery("SELECT name, enabled FROM services", null)
        c.use {
            while (it.moveToNext()) {
                result += it.getString(0) to (it.getInt(1) == 1)
            }
        }
        return result
    }
    fun getAllServicesDetailed(): List<Triple<String, Boolean, Int>> {
        val list = mutableListOf<Triple<String, Boolean, Int>>()
        val c = readableDatabase.rawQuery("SELECT name, enabled, price FROM services", null)
        c.use {
            while (it.moveToNext()) {
                list.add(
                    Triple(
                        it.getString(0),
                        it.getInt(1) == 1,
                        it.getInt(2)
                    )
                )
            }
        }
        return list
    }



    // ============================================================
    // ✅ ROOM HELPERS
    // ============================================================
    fun hasAnyRoom(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM rooms LIMIT 1", null).use { c ->
            return c.moveToFirst()
        }
    }

    fun insertRoomsAuto(count: Int, baseRent: Int, startIndex: Int = 1) {
        if (count <= 0) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO rooms (name, status, baseRent) VALUES (?, 'EMPTY', ?)"
            )
            var idx = startIndex
            var created = 0
            while (created < count) {
                val name = "P%03d".format(idx)
                try {
                    stmt.clearBindings()
                    stmt.bindString(1, name)
                    stmt.bindLong(2, baseRent.toLong())
                    stmt.executeInsert()
                    created++
                } catch (_: Exception) { }
                idx++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
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
    // CONSTANTS
    // ============================================================
    companion object {
        const val DB_NAME = "bsm.db"
        private const val DB_VERSION = 2
    }
}
