package com.example.bsm_management.ui.invoice

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
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

    private var electricKwh = 0
    private var electricRate = 0
    private var waterM3 = 0
    private var waterRate = 0
    private var trashRate = 0
    private var wifiRate = 0
    private var serviceFee = 0

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
            SELECT i.id,
                   i.periodMonth, i.periodYear,
                   i.totalAmount, i.roomRent,
                   i.electricKwh, i.electricRate,
                   i.waterM3, i.waterRate,
                   i.trashRate, i.wifiRate,
                   i.serviceFee,
                   i.createdAt, i.dueAt, i.reason,
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

            electricKwh  = c.getInt(5)
            electricRate = c.getInt(6)
            waterM3      = c.getInt(7)
            waterRate    = c.getInt(8)
            trashRate    = c.getInt(9)
            wifiRate     = c.getInt(10)
            serviceFee   = c.getInt(11)

            createdAt   = c.getLong(12)
            dueAt       = c.getLong(13)
            reason      = c.getString(14)

            roomName    = c.getString(15)
            tenantName  = c.getString(16)
            tenantPhone = c.getString(17)
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

        findViewById<TextView>(R.id.tvElectric).text =
            "${electricKwh} × ${vn.format(electricRate)} = ${vn.format(electricKwh * electricRate)} đ"

        findViewById<TextView>(R.id.tvWater).text =
            "${waterM3} × ${vn.format(waterRate)} = ${vn.format(waterM3 * waterRate)} đ"

        findViewById<TextView>(R.id.tvTrash).text =
            "${vn.format(trashRate)} đ"

        findViewById<TextView>(R.id.tvWifi).text =
            "${vn.format(wifiRate)} đ"

        findViewById<TextView>(R.id.tvService).text =
            "${vn.format(serviceFee)} đ"

        // Tổng cộng & phải thu
        findViewById<TextView>(R.id.tvTotal).text  = "${vn.format(totalAmount)} đ"
    }

    /* ====== ACTIONS ====== */
    private fun doPrint() {
        // chuyển hóa đơn thành View
        val view = inflateInvoiceView()

        // render thành bitmap
        val bmp = renderBitmap(view)

        // In bằng PrintHelper
        val printHelper = androidx.print.PrintHelper(this)
        printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap("Hoá đơn phòng", bmp)
    }



    private fun doShare() {
        // 1) Inflate layout hình hóa đơn
        val view = inflateInvoiceView()

        // 2) Render ảnh
        val bmp = renderBitmap(view)

        // 3) Lưu file vào cache
        val file = saveBitmapFile(bmp)

        // 4) Lấy URI bằng FileProvider
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        // 5) Share ảnh
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(share, "Chia sẻ hình hóa đơn"))
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

    /* ====== RENDER HÓA ĐƠN THÀNH ẢNH ====== */
    private fun inflateInvoiceView(): View {
        val view = layoutInflater.inflate(R.layout.invoice_export_view, null)

        view.findViewById<TextView>(R.id.tvRoom).text = roomName ?: ""
        view.findViewById<TextView>(R.id.tvPeriod).text = "T.$periodMonth, $periodYear"
        view.findViewById<TextView>(R.id.tvCreatedDate).text =
            if (createdAt > 0) df.format(Date(createdAt)) else "—"
        view.findViewById<TextView>(R.id.tvDueDate).text =
            if (dueAt > 0) df.format(Date(dueAt)) else "—"
        view.findViewById<TextView>(R.id.tvTenantName).text = tenantName ?: ""
        view.findViewById<TextView>(R.id.tvTenantPhone).text = "SĐT: ${tenantPhone ?: ""}"

        view.findViewById<TextView>(R.id.tvReason).text =
            reason?.takeIf { it.isNotBlank() } ?: "—"

        view.findViewById<TextView>(R.id.tvRentAmount).text = "${vn.format(rentAmount)} đ"
        view.findViewById<TextView>(R.id.tvDepositAmount).text = "0 đ"
        view.findViewById<TextView>(R.id.tvTimes).text = "1 lần"
        view.findViewById<TextView>(R.id.tvTotalPaid).text = "${vn.format(totalAmount)} đ"

        view.findViewById<TextView>(R.id.tvNote).text =
            "* Chú ý: Vui lòng thanh toán đúng hạn và trước ngày ${if (dueAt > 0) df.format(Date(dueAt)) else "—"}"

        return view
    }

    private fun renderBitmap(view: View): Bitmap {
        val width = 1080
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, width, view.measuredHeight)

        val bmp = Bitmap.createBitmap(width, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)
        return bmp
    }

    private fun saveBitmapFile(bmp: Bitmap): File {
        val file = File(cacheDir, "invoice_$invoiceId.png")
        FileOutputStream(file).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

}
