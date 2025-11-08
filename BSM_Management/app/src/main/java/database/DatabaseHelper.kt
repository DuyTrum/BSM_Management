package database

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bsm_management.ui.message.InboxItem

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // ===== USERS =====
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
        """.trimIndent())

        // ===== ROOMS =====
        db.execSQL("""
            CREATE TABLE rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                floor INTEGER NOT NULL DEFAULT 1,
                status TEXT NOT NULL DEFAULT 'EMPTY',
                baseRent INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent())

        // ===== CONTRACTS =====
        db.execSQL("""
            CREATE TABLE contracts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                roomId INTEGER NOT NULL,
                tenantName TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER,
                deposit INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE
            );
        """.trimIndent())

        // ===== INVOICES =====
        db.execSQL("""
            CREATE TABLE invoices (
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
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
                UNIQUE(roomId, periodYear, periodMonth)
            );
        """.trimIndent())

        // ===== MESSAGES =====
        db.execSQL("""
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT,
                message TEXT,
                time TEXT,
                isRead INTEGER DEFAULT 0
            );
        """.trimIndent())

        // ===== SEED USERS =====
        db.execSQL("""
            INSERT INTO users (phone, name, password) VALUES
            ('12345678','Admin','123456'),
            ('22222222','Nguyen Van A','123456'),
            ('33333333','Le Thi C','123456');
        """.trimIndent())

        // ===== INDEXES =====
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_isRead ON messages(isRead);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.beginTransaction()
        try {
            db.execSQL("DROP TABLE IF EXISTS messages;")
            db.execSQL("DROP TABLE IF EXISTS invoices;")
            db.execSQL("DROP TABLE IF EXISTS contracts;")
            db.execSQL("DROP TABLE IF EXISTS rooms;")
            db.execSQL("DROP TABLE IF EXISTS users;")
            onCreate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ===================== ROOMS =====================

    fun hasAnyRoom(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM rooms LIMIT 1", null).use { c ->
            return c.moveToFirst()
        }
    }

    fun insertRoomsAuto(count: Int, baseRent: Int, startIndex: Int = 1, floor: Int = 1) {
        if (count <= 0) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO rooms (name, floor, status, baseRent) VALUES (?, ?, 'EMPTY', ?)"
            )
            var idx = startIndex
            var created = 0
            while (created < count) {
                val name = "P%03d".format(idx)
                try {
                    stmt.clearBindings()
                    stmt.bindString(1, name)
                    stmt.bindLong(2, floor.toLong())
                    stmt.bindLong(3, baseRent.toLong())
                    stmt.executeInsert()
                    created++
                } catch (_: Exception) {
                    // bỏ qua nếu trùng tên
                }
                idx++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun countRooms(): Long = DatabaseUtils.queryNumEntries(readableDatabase, "rooms")

    /** Xóa toàn bộ phòng và dữ liệu liên quan (contracts, invoices) */
    fun deleteAllRoomsCascade(): Int {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val c = db.rawQuery("SELECT COUNT(*) FROM rooms", null)
            var count = 0
            if (c.moveToFirst()) count = c.getInt(0)
            c.close()
            db.delete("rooms", null, null)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('rooms','contracts','invoices')")
            db.setTransactionSuccessful()
            count
        } finally {
            db.endTransaction()
        }
    }

    // ===================== MESSAGES =====================

    /** Thêm tin nhắn mới */
    fun insertMessage(
        sender: String,
        message: String,
        time: String,
        isRead: Boolean = false
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("sender", sender)
            put("message", message)
            put("time", time)
            put("isRead", if (isRead) 1 else 0)
        }
        db.insert("messages", null, cv)
    }

    /** Lấy toàn bộ tin nhắn */
    fun getAllMessages(): List<InboxItem> {
        val list = mutableListOf<InboxItem>()
        val db = readableDatabase
        val c: Cursor = db.rawQuery(
            "SELECT sender, message, time, isRead FROM messages ORDER BY id DESC",
            null
        )
        c.use {
            while (it.moveToNext()) {
                val sender = it.getString(0) ?: ""
                val msg = it.getString(1) ?: ""
                val time = it.getString(2) ?: ""
                val isRead = it.getInt(3) == 1
                list.add(InboxItem("", sender, msg, time, isRead))
            }
        }
        return list
    }

    /** Xóa 1 tin nhắn */
    fun deleteMessage(item: InboxItem) {
        val db = writableDatabase
        db.delete("messages", "message=? AND time=?", arrayOf(item.message, item.time))
    }

    companion object {
        const val DB_NAME = "bsm.db"
        private const val DB_VERSION = 13 // tăng version để cập nhật schema có messages
    }
}
