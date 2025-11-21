package com.example.bsm_management.ui.invoice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class InvoiceDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "invoiceId"
        fun start(from: android.content.Context, invoiceId: Int) {
            from.startActivity(Intent(from, InvoiceDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, invoiceId)
            })
        }
    }

    private val vn: NumberFormat by lazy { NumberFormat.getInstance(Locale("vi", "VN")) }
    private val df: SimpleDateFormat by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // data
    private var invoiceId = -1
    private var roomName: String? = null
    private var tenantName: String? = null
    private var tenantPhone: String? = null
    private var periodMonth = 0
    private var periodYear = 0
    private var createdAt = 0L
    private var dueAt = 0L
    private var totalAmount = 0
    private var rentAmount = 0
    private var reason: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invoice_detail)

        findViewById<android.view.View>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, ins ->
                val bars = ins.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, 0)
                ins
            }
        }

        findViewById<TextView?>(R.id.tvHeaderTitle)?.text = "Chi tiết hóa đơn"
        findViewById<TextView?>(R.id.tvHeaderSubtitle)?.text = "Thông tin chi tiết hóa đơn phòng trọ"
        findViewById<View?>(R.id.ivBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        invoiceId = intent.getIntExtra(EXTRA_ID, -1)
        if (invoiceId <= 0) {
            Toast.makeText(this, "Thiếu mã hóa đơn", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Click 3 nút đầu trang
        findViewById<LinearLayout>(R.id.btnPrint).setOnClickListener { doPrint() }
        findViewById<LinearLayout>(R.id.btnShare).setOnClickListener { doShare() }
        findViewById<LinearLayout>(R.id.btnCall).setOnClickListener { doCall() }

        // Nút xóa cố định dưới
        findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete() }

        // Load dữ liệu
        loadAndBind()
    }

    private fun loadAndBind() {
        val db = DatabaseHelper(this).readableDatabase
        db.rawQuery(
            """
            SELECT i.id, i.periodMonth, i.periodYear, i.totalAmount, i.roomRent, i.createdAt, i.dueAt, i.reason,
                   r.name AS roomName,
                   c.tenantName, c.tenantPhone
            FROM invoices i
            JOIN rooms r ON r.id = i.roomId
            LEFT JOIN contracts c ON c.roomId = i.roomId AND c.active = 1
            WHERE i.id = ?
            """.trimIndent(),
            arrayOf(invoiceId.toString())
        ).use { c ->
            if (!c.moveToFirst()) {
                Toast.makeText(this, "Không tìm thấy hóa đơn #$invoiceId", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            periodMonth = c.getInt(1)
            periodYear  = c.getInt(2)
            totalAmount = c.getInt(3)
            rentAmount  = c.getInt(4)
            createdAt   = c.getLong(5)
            dueAt       = c.getLong(6)
            reason      = c.getString(7)
            roomName    = c.getString(8)
            tenantName  = c.getString(9)
            tenantPhone = c.getString(10)
        }

        // Bind UI
        findViewById<TextView>(R.id.tvRoomName).text  = roomName ?: "Phòng ?"
        findViewById<TextView>(R.id.tvTenant).text    = "Khách thuê: ${tenantName ?: "—"}"
        findViewById<TextView>(R.id.tvPeriod).text    = "T.${periodMonth}, $periodYear"
        findViewById<TextView>(R.id.tvCreatedAt).text = if (createdAt > 0) df.format(Date(createdAt)) else "—"
        findViewById<TextView>(R.id.tvDueAt).apply {
            text = if (dueAt > 0) df.format(Date(dueAt)) else "—"
            // tô đỏ nếu quá hạn
            if (dueAt > 0 && System.currentTimeMillis() > dueAt) setTextColor(getColor(R.color.err))
        }
        findViewById<TextView>(R.id.tvReason).text    = reason?.takeIf { it.isNotBlank() } ?: "—"

        // Mục tiền thuê phòng: hiển thị số tiền (nếu có khoảng ngày chi tiết thì bạn set thêm vào title)
        findViewById<TextView>(R.id.tvRentAmount).text = "${vn.format(rentAmount.coerceAtLeast(0))} đ"

        // Tổng cộng & phải thu
        findViewById<TextView>(R.id.tvTotal).text  = "${vn.format(totalAmount)} đ"
        findViewById<TextView>(R.id.tvRemain).text = "Tổng phải thu\n${vn.format(totalAmount)} đ"
    }

    /* ====== ACTIONS ====== */
    private fun doPrint() {
        // Chừa hook cho in PDF. Tạm thời thông báo.
        Toast.makeText(this, "Chức năng In phiếu: sẽ xuất PDF/biên lai", Toast.LENGTH_SHORT).show()
    }

    private fun doShare() {
        val msg = buildString {
            appendLine("Hóa đơn: ${roomName ?: ""} (T.$periodMonth/$periodYear)")
            appendLine("Tổng tiền: ${vn.format(totalAmount)} đ")
            appendLine("Hạn nạp: ${if (dueAt > 0) df.format(Date(dueAt)) else "—"}")
            appendLine("Lý do: ${reason ?: "—"}")
            if (!tenantName.isNullOrBlank()) appendLine("Khách thuê: $tenantName")
        }
        val it = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
        }
        startActivity(Intent.createChooser(it, "Gửi hóa đơn"))
    }

    private fun doCall() {
        val phone = tenantPhone?.trim().orEmpty()
        if (phone.isEmpty()) {
            Toast.makeText(this, "Chưa có số điện thoại khách thuê", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Xóa hóa đơn")
            .setMessage("Bạn chắc chắn muốn xóa hóa đơn #$invoiceId?")
            .setPositiveButton("Xóa") { _, _ -> deleteInvoice() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteInvoice() {
        com.example.bsm_management.bg.ReminderScheduler
            .cancelDueReminder(this, invoiceId)

        val db = DatabaseHelper(this).writableDatabase
        val rows = db.delete("invoices", "id=?", arrayOf(invoiceId.toString()))
        if (rows > 0) {
            Toast.makeText(this, "Đã xóa hóa đơn", Toast.LENGTH_SHORT).show()
            // Có thể gửi broadcast nội bộ để list refresh
            // sendBroadcast(Intent("com.example.bsm_management.ACTION_INVOICE_UPDATED"))
            finish()
        } else {
            Toast.makeText(this, "Xóa thất bại", Toast.LENGTH_SHORT).show()
        }
    }
}
