// database/DatabaseHelper.kt
package database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Tạo đầy đủ schema
        createCoreTables(db)
        createHostelTableIfMissing(db)
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nếu bạn từng dùng phiên bản rất cũ muốn reset sạch:
        if (oldVersion < 5) {
            db.beginTransaction()
            try {
                dropIndexes(db)
                db.execSQL("DROP TABLE IF EXISTS invoices;")
                db.execSQL("DROP TABLE IF EXISTS contracts;")
                db.execSQL("DROP TABLE IF EXISTS rooms;")
                db.execSQL("DROP TABLE IF EXISTS messages;")
                db.execSQL("DROP TABLE IF EXISTS hostels;")
                db.execSQL("DROP TABLE IF EXISTS users;")
                onCreate(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            return
        }

        // 🔧 Từ 5 trở lên: không drop, chỉ "heal" – tạo bảng/ chỉ mục nếu thiếu
        createCoreTables(db)            // dùng IF NOT EXISTS nên an toàn
        createHostelTableIfMissing(db)  // đảm bảo có hostels
        createIndexes(db)               // đảm bảo có index
    }

    // ---- Helpers ----

    private fun createCoreTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                floor INTEGER NOT NULL DEFAULT 1,
                status TEXT NOT NULL DEFAULT 'EMPTY',
                baseRent INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS contracts (
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
                createdAt INTEGER NOT NULL DEFAULT (strftime('%s','now')*1000),
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
                UNIQUE(roomId, periodYear, periodMonth)
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT,
                tag TEXT,
                pinned INTEGER NOT NULL DEFAULT 0,
                unread INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            );
        """.trimIndent())
    }

    private fun createHostelTableIfMissing(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS hostels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                type TEXT NOT NULL DEFAULT 'HOSTEL',
                rentMode TEXT NOT NULL DEFAULT 'ROOM',
                autoGenerate INTEGER NOT NULL DEFAULT 1,
                sampleRooms INTEGER NOT NULL DEFAULT 0,
                sampleArea INTEGER NOT NULL DEFAULT 0,
                samplePrice INTEGER NOT NULL DEFAULT 0,
                maxPeople INTEGER NOT NULL DEFAULT 0,
                invoiceDay INTEGER NOT NULL DEFAULT 1,
                dueDays INTEGER NOT NULL DEFAULT 5,
                createdAt INTEGER NOT NULL
            );
        """.trimIndent())
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_createdAt ON messages(createdAt);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_pinned ON messages(pinned);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_hostels_createdAt ON hostels(createdAt);")
    }

    private fun dropIndexes(db: SQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS idx_contracts_active;")
        db.execSQL("DROP INDEX IF EXISTS idx_invoices_paid;")
        db.execSQL("DROP INDEX IF EXISTS idx_messages_createdAt;")
        db.execSQL("DROP INDEX IF EXISTS idx_messages_pinned;")
        db.execSQL("DROP INDEX IF EXISTS idx_hostels_createdAt;")
    }

    // 👉 Dùng để MainActivity router quyết định vào AddHostel hay Dashboard
    fun hasHostel(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM hostels LIMIT 1", null).use { c ->
            return c.moveToFirst()
        }
    }

    companion object {
        const val DB_NAME: String = "bsm.db"
        private const val DB_VERSION = 6  // bump để chắc chắn chạy onUpgrade heal schema
    }
}
