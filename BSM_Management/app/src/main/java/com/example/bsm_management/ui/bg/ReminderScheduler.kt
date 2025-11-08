package com.example.bsm_management.bg

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bsm_management.receiver.InvoiceDueReceiver
import com.example.bsm_management.service.NotificationService

object ReminderScheduler {

    fun scheduleDueReminder(ctx: Context, invoiceId: Int, roomName: String?, dueAt: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 👉 Tạo intent broadcast đến Receiver
        val i = Intent(ctx, InvoiceDueReceiver::class.java).apply {
            action = "ACTION_INVOICE_DUE"
            putExtra("invoiceId", invoiceId)
            putExtra("roomName", roomName ?: "Phòng ?")
        }

        val pi = PendingIntent.getBroadcast(
            ctx, invoiceId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                Log.w("ReminderScheduler", "Chưa có quyền SCHEDULE_EXACT_ALARM → dùng inexact alarm.")
                am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
            }

            // Khi đặt lịch nhắc, khởi động Service chạy nền thông báo
            val serviceIntent = Intent(ctx, NotificationService::class.java).apply {
                putExtra("ROOM_NAME", roomName ?: "Phòng ?")
            }
            ctx.startService(serviceIntent)

            Log.i("ReminderScheduler", "Đã đặt báo nhắc hạn cho hóa đơn #$invoiceId lúc $dueAt")

        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "Thiếu quyền exact alarm: ${e.message}")
            am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
        }
    }

    fun cancelDueReminder(ctx: Context, invoiceId: Int) {
        val i = Intent(ctx, InvoiceDueReceiver::class.java).apply {
            action = "ACTION_INVOICE_DUE"
        }
        val pi = PendingIntent.getBroadcast(
            ctx, invoiceId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
        Log.i("ReminderScheduler", "Đã hủy báo nhắc hạn cho hóa đơn #$invoiceId")
    }
}
