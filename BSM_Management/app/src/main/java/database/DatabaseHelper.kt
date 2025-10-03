package database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Bật ràng buộc khóa ngoại
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // USERS: phục vụ Login/Register (mẫu học tập: lưu plain password)
        db.execSQL(
            "CREATE TABLE users (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  phone TEXT NOT NULL UNIQUE," +
                    "  name TEXT NOT NULL," +
                    "  password TEXT NOT NULL" +
                    ");"
        )

        // ROOMS: phòng trọ
        db.execSQL(
            "CREATE TABLE rooms (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  name TEXT NOT NULL UNIQUE," +
                    "  floor INTEGER NOT NULL DEFAULT 1," +
                    "  status TEXT NOT NULL DEFAULT 'EMPTY'," +  // EMPTY | RENTED | MAINT
                    "  baseRent INTEGER NOT NULL DEFAULT 0" +
                    ");"
        )

        // CONTRACTS: hợp đồng (ràng buộc tới rooms)
        db.execSQL(
            "CREATE TABLE contracts (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  roomId INTEGER NOT NULL," +
                    "  tenantName TEXT NOT NULL," +
                    "  startDate INTEGER NOT NULL," +  // epoch millis
                    "  endDate INTEGER," +
                    "  deposit INTEGER NOT NULL DEFAULT 0," +
                    "  active INTEGER NOT NULL DEFAULT 1," +  // 1=true, 0=false
                    "  FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE" +
                    ");"
        )

        // INVOICES: hóa đơn (ràng buộc tới rooms)
        db.execSQL(
            "CREATE TABLE invoices (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  roomId INTEGER NOT NULL," +
                    "  periodYear INTEGER NOT NULL," +
                    "  periodMonth INTEGER NOT NULL," +
                    "  roomRent INTEGER NOT NULL," +
                    "  electricKwh INTEGER NOT NULL DEFAULT 0," +
                    "  waterM3 INTEGER NOT NULL DEFAULT 0," +
                    "  serviceFee INTEGER NOT NULL DEFAULT 0," +
                    "  totalAmount INTEGER NOT NULL," +
                    "  paid INTEGER NOT NULL DEFAULT 0," +  // 1=đã thu, 0=chưa
                    "  createdAt INTEGER NOT NULL DEFAULT (strftime('%s','now')*1000)," +
                    "  FOREIGN KEY(roomId) REFERENCES rooms(id) ON DELETE CASCADE," +
                    "  UNIQUE(roomId, periodYear, periodMonth)" +
                    ");"
        )

        // ===== DỮ LIỆU MẪU =====

        // User mẫu (đăng nhập thử)
        db.execSQL(
            "INSERT INTO users (phone, name, password) VALUES " +
                    "('0900000001','Admin','123456')," +
                    "('0900000002','Nguyen Van A','123456');"
        )

        // 3 phòng mẫu
        db.execSQL(
            "INSERT INTO rooms (name, floor, status, baseRent) VALUES " +
                    "('P101', 1, 'EMPTY', 1500000)," +
                    "('P102', 1, 'RENTED', 1500000)," +
                    "('P201', 2, 'EMPTY', 1800000);"
        )

        // Hợp đồng mẫu cho phòng P102 (giả định id phòng mới tạo lần lượt là 1,2,3)
        db.execSQL(
            "INSERT INTO contracts (roomId, tenantName, startDate, endDate, deposit, active) VALUES " +
                    "(2, 'Tran Thi B', strftime('%s','now','-2 months')*1000, NULL, 2000000, 1);"
        )

        // Hóa đơn mẫu cho P102 (tháng trước và tháng kia)
        db.execSQL(
            "INSERT INTO invoices " +
                    "(roomId, periodYear, periodMonth, roomRent, electricKwh, waterM3, serviceFee, totalAmount, paid) VALUES " +
                    "(2, 2025, 9, 1500000, 45, 8, 50000, (1500000 + 45*3500 + 8*8000 + 50000), 0)," +
                    "(2, 2025, 8, 1500000, 42, 7, 50000, (1500000 + 42*3500 + 7*8000 + 50000), 1);"
        )

        // Index tham khảo (không bắt buộc)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contracts_active ON contracts(active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(paid);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Dev nhanh: drop & create lại (sau này có thể đổi sang ALTER để giữ dữ liệu)
        db.execSQL("DROP TABLE IF EXISTS invoices;")
        db.execSQL("DROP TABLE IF EXISTS contracts;")
        db.execSQL("DROP TABLE IF EXISTS rooms;")
        db.execSQL("DROP TABLE IF EXISTS users;")
        onCreate(db)
    }

    companion object {
        const val DB_NAME: String = "bsm.db"
        private const val DB_VERSION = 1
    }
}