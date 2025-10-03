package database

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // LẦN ĐẦU: tạo file DB & seed dữ liệu; CÁC LẦN SAU: chỉ mở rồi đóng (không xoá)
        DatabaseHelper(applicationContext).writableDatabase.close()
    }
}