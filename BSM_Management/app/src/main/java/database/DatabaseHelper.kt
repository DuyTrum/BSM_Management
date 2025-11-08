package database

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bsm_management.ui.contract.Contract
import android.database.DatabaseUtils

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                status TEXT NOT NULL DEFAULT 'EMPTY',
                baseRent INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE contracts (
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
                paid INTEGER NOT NULL DEFAULT 0, -- 1=đã thu, 0=chưa, 2=hủy
                createdAt INTEGER NOT NULL,
                dueAt INTEGER,
                reason TEXT,
                FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE,
                UNIQUE(roomId, periodYear, periodMonth)
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT,
                receiver TEXT,
                content TEXT,
                pinned INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            );
        """.trimIndent())

        // ===== SEED DATA =====

        db.execSQL("""
            INSERT INTO users (phone, name, password) VALUES
            ('12345678','Admin','123456'),
            ('22222222','Nguyen Van A','123456'),
            ('33333333','Le Thi C','123456');
        """.trimIndent())

        db.execSQL("""
            INSERT INTO rooms (name, status, baseRent) VALUES
            ('P101', 'RENTED', 1500000),
            ('P102', 'RENTED', 1500000),
            ('P201', 'EMPTY', 1800000),
            ('P202', 'RENTED', 1800000),
            ('P203', 'EMPTY', 1800000);
        """.trimIndent())

        db.execSQL("""
            INSERT INTO contracts (roomId, tenantName, tenantPhone, startDate, endDate, deposit, active) VALUES
            (1, 'Le Van C',  '0901111111', strftime('%s','now','-5 months')*1000, NULL, 2000000, 1),
            (2, 'Tran Thi B','0902222222', strftime('%s','now','-2 months')*1000, NULL, 2000000, 1),
            (4, 'Pham D',    '0903333333', strftime('%s','now','-1 months')*1000, NULL, 2500000, 1);
        """.trimIndent())

        db.execSQL("""
            INSERT INTO invoices
                (roomId, periodYear, periodMonth, roomRent, electricKwh, waterM3, serviceFee,
                 totalAmount, paid, createdAt, dueAt, reason)
            VALUES
                (1, 2025, 7, 1500000, 40, 7, 50000,
                 (1500000 + 40*3500 + 7*8000 + 50000), 1,
                 strftime('%s','now','-85 days')*1000,
                 strftime('%s','now','-55 days')*1000,
                 'Thanh toán định kỳ'),

                (1, 2025, 8, 1500000, 43, 7, 50000,
                 (1500000 + 43*3500 + 7*8000 + 50000), 1,
                 strftime('%s','now','-55 days')*1000,
                 strftime('%s','now','-25 days')*1000,
                 'Thanh toán định kỳ'),

                (1, 2025, 9, 1500000, 45, 8, 50000,
                 (1500000 + 45*3500 + 8*8000 + 50000), 0,
                 strftime('%s','now','-25 days')*1000,
                 strftime('%s','now','+5 days')*1000,
                 'Thanh toán định kỳ'),

                (2, 2025, 8, 1500000, 42, 7, 50000,
                 (1500000 + 42*3500 + 7*8000 + 50000), 1,
                 strftime('%s','now','-60 days')*1000,
                 strftime('%s','now','-30 days')*1000,
                 'Thanh toán định kỳ'),

                (2, 2025, 9, 1500000, 45, 8, 50000,
                 (1500000 + 45*3500 + 8*8000 + 50000), 0,
                 strftime('%s','now','-20 days')*1000,
                 strftime('%s','now','+10 days')*1000,
                 'Thanh toán định kỳ'),

                (4, 2025, 10, 1800000, 38, 6, 60000,
                 (1800000 + 38*3500 + 6*8000 + 60000), 0,
                 strftime('%s','now')*1000,
                 strftime('%s','now','+30 days')*1000,
                 'Thanh toán định kỳ'),

                (4, 2025, 9, 1800000, 36, 6, 60000,
                 (1800000 + 36*3500 + 6*8000 + 60000), 1,
                 strftime('%s','now','-18 days')*1000,
                 strftime('%s','now','+12 days')*1000,
                 'Thanh toán định kỳ');
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_isRead ON messages(isRead);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            db.beginTransaction()
            try {
                db.execSQL("DROP INDEX IF EXISTS idx_hostels_createdAt;")
                db.execSQL("DROP TABLE IF EXISTS hostels;")
                onCreate(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            return
        }
        onCreate(db)
    }

    // ======= Helpers =======

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
            count
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val DB_NAME = "bsm.db"
        private const val DB_VERSION = 1  // giữ nguyên
    }
}
