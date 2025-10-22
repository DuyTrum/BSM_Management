package database

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        createChannels()

        DatabaseHelper(applicationContext).writableDatabase.close()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = "invoice_reminders"
            val name = "Nhắc hạn hóa đơn"
            val desc = "Thông báo đến hạn thu tiền phòng"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val ch = NotificationChannel(id, name, importance).apply {
                description = desc
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }
}
