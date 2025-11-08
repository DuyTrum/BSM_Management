package com.example.bsm_management.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bsm_management.R
import com.example.bsm_management.ui.invoice.InvoiceDetailActivity
import com.example.bsm_management.ui.invoice.InvoiceListActivity

class InvoiceDueReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != "ACTION_INVOICE_DUE") return
        val id = intent.getIntExtra("invoiceId", -1)
        val room = intent.getStringExtra("roomName") ?: "Phòng ?"

        val open = Intent(ctx, InvoiceDetailActivity::class.java).apply {
            putExtra("focusInvoiceId", id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val contentPi = PendingIntent.getActivity(
            ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(ctx, "invoice_reminders")
            .setSmallIcon(R.drawable.ic_bill_due)
            .setContentTitle("Đến hạn thu tiền: $room")
            .setContentText("Hóa đơn đến hạn hôm nay. Nhấn để mở danh sách.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
