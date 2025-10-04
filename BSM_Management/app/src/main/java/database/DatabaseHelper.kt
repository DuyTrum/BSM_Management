// database/DatabaseHelper.kt
package database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.DatabaseUtils

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // ===== Schema chính (KHÔNG có hostels) =====
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
        """.trimIndent())

        // ROOMS
        db.execSQL("""
            CREATE TABLE rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                floor INTEGER NOT NULL DEFAULT 1,
                status TEXT NOT NULL DEFAULT 'EMPTY',
                baseRent INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent())

        // CONTRACTS
        db.execSQL("""
            CREATE TABLE contracts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                roomId INTEGER NOT NULL,
                tenantName TEXT NOT NULL,
                startDate INTEGER NOT NULL, -- epoch millis
                endDate INTEGER,
                deposit INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE
            );
        """.trimIndent())

        // INVOICES
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
                paid INTEGER NOT NULL DEFAULT 0, -- 1=đã thu, 0=chưa
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
                UNIQUE(roomId, periodYear, periodMonth)
            );
        """.trimIndent())

        // ===== SEED DATA =====

        // Users: số dễ nhập
        db.execSQL("""
            INSERT INTO users (phone, name, password) VALUES
            ('12345678','Admin','123456'),
            ('22222222','Nguyen Van A','123456'),
            ('33333333','Le Thi C','123456');
        """.trimIndent())

        // Indexes (không có hostels)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_createdAt ON messages(createdAt);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_pinned ON messages(pinned);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Heal schema – đảm bảo không còn hostels legacy
        if (oldVersion < 7) {
            db.beginTransaction()
            try {
                // xoá index cũ nếu có
                db.execSQL("DROP INDEX IF EXISTS idx_hostels_createdAt;")
                // xoá table hostels nếu tồn tại
                db.execSQL("DROP TABLE IF EXISTS hostels;")
                // re-create core schema & indexes
                onCreate(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            return
        }

        // các bản vá về sau (nếu có) – hiện tại không cần
        onCreate(db) // chạy IF NOT EXISTS an toàn
    }

    // ======= Helpers cho flow chỉ dùng rooms =======

    /** Có phòng nào trong DB chưa? */
    fun hasAnyRoom(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM rooms LIMIT 1", null).use { c ->
            return c.moveToFirst()
        }
    }

    /** Tạo N phòng tự động: P001..Pnnn, floor=1, status='EMPTY', baseRent=price */
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
                val name = "P%03d".format(idx) // P001, P002, ...
                try {
                    stmt.clearBindings()
                    stmt.bindString(1, name)
                    stmt.bindLong(2, floor.toLong())
                    stmt.bindLong(3, baseRent.toLong())
                    stmt.executeInsert()
                    created++
                } catch (_: Exception) {
                    // trùng tên -> tăng số tiếp
                }
                idx++
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    // database/DatabaseHelper.kt  (thêm vào trong class)
    /** Đếm số phòng hiện có */
    fun countRooms(): Long =
        DatabaseUtils.queryNumEntries(readableDatabase, "rooms")

    /** Xóa toàn bộ phòng và dữ liệu liên quan (contracts, invoices) */
    fun deleteAllRoomsCascade(): Int {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Đếm trước để trả về
            val current = countRooms().toInt()

            // Xóa rooms -> sẽ tự cascade sang contracts/invoices
            db.delete("rooms", null, null)

            // Reset auto-increment (nếu muốn)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('rooms','contracts','invoices')")

            db.setTransactionSuccessful()
            current
        } finally {
            db.endTransaction()
        }
    }


    companion object {
        const val DB_NAME: String = "bsm.db"
        private const val DB_VERSION = 7  // bump để xoá hostels nếu còn sót
    }
}
