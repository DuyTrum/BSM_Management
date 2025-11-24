package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InvoiceActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private var curMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var curYear  = Calendar.getInstance().get(Calendar.YEAR)

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

        // --- Month navigation ---
        val tvMonth = findViewById<TextView>(R.id.tvMonth)
        val btnPrev = findViewById<View>(R.id.btnPrev)
        val btnNext = findViewById<View>(R.id.btnNext)

        updateMonthLabel(tvMonth)

        btnPrev.setOnClickListener {
            curMonth--
            if (curMonth == 0) {
                curMonth = 12
                curYear--
            }
            updateMonthLabel(tvMonth)
            reload()
        }

        btnNext.setOnClickListener {
            curMonth++
            if (curMonth == 13) {
                curMonth = 1
                curYear++
            }
            updateMonthLabel(tvMonth)
            reload()
        }

        tvMonth.setOnClickListener {
            showMonthPicker(tvMonth)
        }


        // Views
        rv = findViewById(R.id.rvInvoices)
        tvEmpty = findViewById(R.id.tvEmpty) // nhớ đã thêm vào activity_in_voice.xml

        adapter = InvoiceAdapter { item ->
            checkBeforeCreateInvoice(item)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        reload()
    }

    private fun checkBeforeCreateInvoice(room: RoomItem) {

        val count = room.invoiceCount

        if (count > 0) {
            AlertDialog.Builder(this)
                .setTitle("Đã có hóa đơn")
                .setMessage(
                    "Phòng ${room.roomName} đã có $count hóa đơn trong tháng $curMonth/$curYear.\n" +
                            "Bạn có chắc muốn lập thêm hóa đơn nữa không?"
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lập tiếp") { _, _ ->
                    openCreateInvoice(room)
                }
                .show()
        } else {
            openCreateInvoice(room)
        }
    }

    private fun openCreateInvoice(room: RoomItem) {
        val i = Intent(this, AddInvoiceActivity::class.java)
        i.putExtra("roomId", room.roomId)
        i.putExtra("roomName", room.roomName)
        i.putExtra("rent", room.rent)
        startActivity(i)
    }

    private fun updateMonthLabel(tv: TextView) {
        tv.text = "Tháng $curMonth, $curYear"
    }
    private fun showMonthPicker(tv: TextView) {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                curYear = year
                curMonth = month + 1
                updateMonthLabel(tv)
                reload()
            },
            curYear,
            curMonth - 1,
            1
        )

        dialog.setOnShowListener {
            val dp = dialog.datePicker
            val dayId = resources.getIdentifier("day", "id", "android")
            dp.findViewById<View>(dayId)?.visibility = View.GONE
        }

        dialog.show()
    }

    private fun reload() {
        val data = loadRoomsFromDb()
        adapter.submitList(data)
        tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Lấy danh sách phòng chỉ khi hợp đồng còn hiệu lực (theo thời gian) */
    private fun loadRoomsFromDb(): List<RoomItem> {
        val db = DatabaseHelper(this).readableDatabase

        // Tính mốc thời gian đầu & cuối tháng được chọn
        val calStart = Calendar.getInstance().apply {
            set(curYear, curMonth - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(curYear, curMonth - 1, 1)
            add(Calendar.MONTH, 1)
            set(Calendar.MILLISECOND, 0)
        }

        val monthStart = calStart.timeInMillis
        val monthEnd = calEnd.timeInMillis

        val sql = """
        SELECT r.id, r.name, r.baseRent, r.status,
               c.tenantName, c.startDate, c.endDate
        FROM rooms r
        INNER JOIN contracts c
            ON c.roomId = r.id
           AND c.startDate <= $monthEnd
           AND (c.endDate IS NULL OR c.endDate >= $monthStart)
        ORDER BY r.name
    """.trimIndent()

        val list = mutableListOf<RoomItem>()
        db.rawQuery(sql, null).use { cur ->
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
                    else    -> "Đang ở"
                }
                val contract = "$startStr - $endStr"
                val tenantCount = DatabaseUtils.longForQuery(
                    db,
                    "SELECT COUNT(*) FROM tenants WHERE roomId = ? AND isOld = 0",
                    arrayOf(id.toString())
                ).toInt()

                // --- Lấy maxPeople từ bảng rooms ---
                val maxPeople = DatabaseUtils.longForQuery(
                    db,
                    "SELECT maxPeople FROM rooms WHERE id = ?",
                    arrayOf(id.toString())
                ).toInt()

                // Đếm số hóa đơn của phòng trong tháng đang chọn
                val invoiceCount = DatabaseUtils.longForQuery(
                    db,
                    """
                        SELECT COUNT(*) FROM invoices 
                        WHERE roomId = ? AND periodMonth = ? AND periodYear = ?
                    """.trimIndent(),
                        arrayOf(id.toString(), curMonth.toString(), curYear.toString())
                    ).toInt()

                list.add(
                    RoomItem(
                        roomId   = id,
                        roomName = name,
                        phone    = if (tenant.isNullOrBlank()) "—" else tenant,
                        contract = contract,
                        status   = status,
                        rent     = "${formatVnd(baseRent)}đ",
                        people   = "${tenantCount}/${maxPeople} người",
                        invoiceCount = invoiceCount
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
