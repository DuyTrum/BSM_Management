package com.example.bsm_management.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bsm_management.R
import com.example.bsm_management.ui.invoice.InvoiceListActivity

class NotificationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val roomName = intent?.getStringExtra("ROOM_NAME") ?: "Phòng ?"

        // Tạo kênh thông báo (chỉ cần 1 lần duy nhất)
        val channelId = "invoice_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc hạn hóa đơn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Thông báo khi đến hạn thu tiền phòng" }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        // Intent mở danh sách hóa đơn khi người dùng bấm thông báo
        val openIntent = Intent(this, InvoiceListActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo thông báo
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bill_due)
            .setContentTitle("Nhắc thu tiền: $roomName")
            .setContentText("Hóa đơn phòng này đến hạn thu tiền.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()

        // Gửi thông báo
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(roomName.hashCode(), notification)

        // Dừng Service sau khi hiển thị thông báo
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
