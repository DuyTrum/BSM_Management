package com.example.bsm_management.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.bsm_management.bg.ReminderScheduler
import database.DatabaseHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val am = ctx.getSystemService(AlarmManager::class.java)
        // 🔒 Kiểm tra quyền trước khi đặt báo chính xác
        if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Không có quyền -> không crash, chỉ log
            android.util.Log.w("BootReceiver", "App chưa được cấp quyền SCHEDULE_EXACT_ALARM, bỏ qua đặt lại báo.")
            return
        }

        // 🕓 Có quyền -> đặt lại báo cho các hóa đơn chưa đến hạn
        val now = System.currentTimeMillis()
        val db = DatabaseHelper(ctx).readableDatabase
        db.rawQuery(
            """
            SELECT i.id, i.dueAt, r.name
            FROM invoices i
            JOIN rooms r ON r.id = i.roomId
            WHERE i.paid = 0 AND i.dueAt > ?
            """.trimIndent(),
            arrayOf(now.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val invId = c.getInt(0)
                val due = c.getLong(1)
                val room = c.getString(2)
                ReminderScheduler.scheduleDueReminder(ctx, invId, room, due)
            }
        }
    }
}
