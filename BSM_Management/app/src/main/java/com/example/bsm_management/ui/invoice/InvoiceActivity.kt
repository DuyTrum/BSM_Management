package com.example.bsm_management.ui.invoice

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_in_voice)

        // Fit system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Header
        findViewById<TextView>(R.id.tvHeaderTitle).text = getString(R.string.invoice_create_title)
        findViewById<TextView>(R.id.tvHeaderSubtitle).text = getString(R.string.invoice_create_subtitle)
        findViewById<View>(R.id.ivBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Views
        rv = findViewById(R.id.rvInvoices)
        tvEmpty = findViewById(R.id.tvEmpty) // nhớ đã thêm vào activity_in_voice.xml

        // RecyclerView + Adapter
        adapter = InvoiceAdapter { item ->
            val i = Intent(this, AddInvoiceActivity::class.java)
            i.putExtra("roomId", item.roomId)
            i.putExtra("roomName", item.roomName)
            i.putExtra("rent", item.rent)
            startActivity(i)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // Load lần đầu
        reload()
    }

    private fun reload() {
        val data = loadRoomsFromDb()
        adapter.submitList(data)
        tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Lấy danh sách phòng chỉ khi hợp đồng còn hiệu lực (theo thời gian) */
    private fun loadRoomsFromDb(): List<RoomItem> {
        val db = DatabaseHelper(this).readableDatabase
        val sql = """
            SELECT r.id, r.name, r.baseRent, r.status,
                   c.tenantName, c.startDate, c.endDate
            FROM rooms r
            INNER JOIN contracts c
                ON c.roomId = r.id
               AND c.startDate <= strftime('%s','now')*1000
               AND (c.endDate IS NULL OR c.endDate >= strftime('%s','now')*1000)
            ORDER BY r.name
        """.trimIndent()

        val list = mutableListOf<RoomItem>()
        db.rawQuery(sql, null).use { cur: Cursor ->
            val idxId     = cur.getColumnIndexOrThrow("id")
            val idxName   = cur.getColumnIndexOrThrow("name")
            val idxBase   = cur.getColumnIndexOrThrow("baseRent")
            val idxStatus = cur.getColumnIndexOrThrow("status")
            val idxTenant = cur.getColumnIndexOrThrow("tenantName")
            val idxStart  = cur.getColumnIndexOrThrow("startDate")
            val idxEnd    = cur.getColumnIndexOrThrow("endDate")

            while (cur.moveToNext()) {
                val id        = cur.getInt(idxId)
                val name      = cur.getString(idxName)
                val baseRent  = cur.getInt(idxBase)
                val statusRaw = cur.getString(idxStatus)
                val tenant    = cur.getString(idxTenant)
                val startStr  = formatDate(cur.getLong(idxStart))
                val endStr    = if (!cur.isNull(idxEnd)) formatDate(cur.getLong(idxEnd)) else "Vô thời hạn"

                val status = when (statusRaw) {
                    "EMPTY" -> "Trống"
                    "MAINT" -> "Bảo trì"
                    else    -> "Đang ở" // vì đã có HĐ hiệu lực
                }
                val contract = "$startStr - $endStr"

                list.add(
                    RoomItem(
                        roomId   = id,
                        roomName = name,
                        phone    = if (tenant.isNullOrBlank()) "—" else tenant, // chưa có phone → tạm hiển thị tên
                        contract = contract,
                        status   = status,
                        rent     = "${formatVnd(baseRent)}đ",
                        people   = "1/1 người" // TODO: map số người thực nếu có trong DB
                    )
                )
            }
        }
        return list
    }

    private fun formatVnd(value: Int): String =
        NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(value)

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMillis))
}
