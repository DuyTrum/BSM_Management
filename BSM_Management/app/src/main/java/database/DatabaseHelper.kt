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
        // USERS
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

        // Rooms: 5 phòng (1,2,4 đang thuê)
        db.execSQL("""
            INSERT INTO rooms (name, floor, status, baseRent) VALUES
            ('P101', 1, 'RENTED', 1500000),
            ('P102', 1, 'RENTED', 1500000),
            ('P201', 2, 'EMPTY', 1800000),
            ('P202', 2, 'RENTED', 1800000),
            ('P203', 2, 'EMPTY', 1800000);
        """.trimIndent())

        // Contracts cho phòng đang thuê (giả định id phòng bắt đầu từ 1..5)
        db.execSQL("""
            INSERT INTO contracts (roomId, tenantName, startDate, endDate, deposit, active) VALUES
            (1, 'Le Van C',  strftime('%s','now','-5 months')*1000, NULL, 2000000, 1),
            (2, 'Tran Thi B', strftime('%s','now','-2 months')*1000, NULL, 2000000, 1),
            (4, 'Pham D',     strftime('%s','now','-1 months')*1000, NULL, 2500000, 1);
        """.trimIndent())

        // Invoices: nhiều tháng cho các phòng thuê
        db.execSQL("""
            INSERT INTO invoices
                (roomId, periodYear, periodMonth, roomRent, electricKwh, waterM3, serviceFee, totalAmount, paid, createdAt)
            VALUES
                -- P101 (roomId=1): 7,8,9/2025
                (1, 2025, 7, 1500000, 40, 7, 50000, (1500000 + 40*3500 + 7*8000 + 50000), 1, strftime('%s','now','-85 days')*1000),
                (1, 2025, 8, 1500000, 43, 7, 50000, (1500000 + 43*3500 + 7*8000 + 50000), 1, strftime('%s','now','-55 days')*1000),
                (1, 2025, 9, 1500000, 45, 8, 50000, (1500000 + 45*3500 + 8*8000 + 50000), 0, strftime('%s','now','-25 days')*1000),

                -- P102 (roomId=2): 8,9/2025
                (2, 2025, 8, 1500000, 42, 7, 50000, (1500000 + 42*3500 + 7*8000 + 50000), 1, strftime('%s','now','-60 days')*1000),
                (2, 2025, 9, 1500000, 45, 8, 50000, (1500000 + 45*3500 + 8*8000 + 50000), 0, strftime('%s','now','-20 days')*1000),

                -- P202 (roomId=4): 9,10/2025 (tháng 10 mới tạo)
                (4, 2025,10, 1800000, 38, 6, 60000, (1800000 + 38*3500 + 6*8000 + 60000), 0, strftime('%s','now')*1000),
                (4, 2025, 9, 1800000, 36, 6, 60000, (1800000 + 36*3500 + 6*8000 + 60000), 1, strftime('%s','now','-18 days')*1000);
        """.trimIndent())

        // Indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS invoices;")
        db.execSQL("DROP TABLE IF EXISTS contracts;")
        db.execSQL("DROP TABLE IF EXISTS rooms;")
        db.execSQL("DROP TABLE IF EXISTS users;")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "bsm.db"
        private const val DB_VERSION = 1
    }
}
