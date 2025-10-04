package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class AddInvoiceActivity : AppCompatActivity() {

    private lateinit var spReason: Spinner
    private lateinit var tvDueDate: TextView
    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var tvMonthDay: TextView
    private lateinit var tvAmountPerMonth: TextView
    private lateinit var tvSubTotal: TextView
    private lateinit var btnCreate: Button

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val vn = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))

    private val reasons = arrayOf(
        "Thu tiền hàng tháng",
        "Thu tiền cọc",
        "Hoàn tiền cọc",
        "Thu tiền kết thúc hợp đồng"
    )

    // Data từ InvoiceActivity
    private var roomId: Int = -1
    private var roomName: String? = null
    private var baseRent: Int = 0      // lấy từ DB theo roomId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_invoice)

        // Insets cho root id=main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // ---- Nhận dữ liệu từ Intent ----
        roomId = intent.getIntExtra("roomId", -1)
        roomName = intent.getStringExtra("roomName")
        // Nếu bên kia có truyền "rent" dạng "3.000.000đ", bạn vẫn lấy baseRent từ DB cho chính xác

        // ---- View refs ----
        spReason         = findViewById(R.id.spReason)
        tvDueDate        = findViewById(R.id.tvDueDate)
        tvFromDate       = findViewById(R.id.tvFromDate)
        tvToDate         = findViewById(R.id.tvToDate)
        tvMonthDay       = findViewById(R.id.tvMonthDay)
        tvAmountPerMonth = findViewById(R.id.tvAmountPerMonth)
        tvSubTotal       = findViewById(R.id.tvSubTotal)
        btnCreate        = findViewById(R.id.btnCreateInvoice)

        // Header
        setupHeader(
            title = "Lập hóa đơn: ${roomName ?: queryRoomName(roomId) ?: "Phòng ?"}",
            subtitle = "Điền thông tin và chọn ngày"
        )

        // Lấy baseRent từ DB
        baseRent = queryRoomBaseRent(roomId)
        updateAmountLabels() // set “Thành tiền: …/tháng” theo baseRent ban đầu

        // Spinner lý do
        setupReasonSpinnerAsDialog()

        // Date pickers
        setupDatePickers()

        // Tính tháng/ngày + cập nhật tổng tiền ban đầu
        recomputeMonthDay()     // tính text “x tháng, y ngày”
        updateSubtotal()        // tính tiền theo khoảng ngày

        // Ghi hóa đơn
        btnCreate.setOnClickListener { createInvoice() }
    }

    /* ---------------------- Header ---------------------- */
    private fun setupHeader(title: String, subtitle: String) {
        val header: View = findViewById(R.id.headerBack)
        val tvTitle   = header.findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSub     = header.findViewById<TextView>(R.id.tvHeaderSubtitle)
        val btnBack   = header.findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = title
        tvSub.text   = subtitle
        btnBack.setOnClickListener { finish() }
    }

    /* ---------------------- Spinner: lý do ---------------------- */
    private fun setupReasonSpinnerAsDialog() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spReason.adapter = adapter
        spReason.setSelection(0)

        spReason.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP) showReasonDialog()
            true
        }
        spReason.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showReasonDialog()
        }
    }

    private fun showReasonDialog() {
        val current = spReason.selectedItemPosition.takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(this)
            .setTitle("Chọn lý do lập hóa đơn")
            .setSingleChoiceItems(reasons, current) { dialog, which ->
                spReason.setSelection(which)
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /* ---------------------- Date pickers ---------------------- */
    private fun setupDatePickers() {
        // gợi ý hạn đóng tiền: +7 ngày từ hôm nay
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        tvDueDate.text = df.format(cal.time)

        tvDueDate.setOnClickListener { openDatePickerFor(tvDueDate) }
        tvFromDate.setOnClickListener {
            openDatePickerFor(tvFromDate) {
                ensureToNotBeforeFrom()
                recomputeMonthDay()
                updateSubtotal()
            }
        }
        tvToDate.setOnClickListener {
            openDatePickerFor(tvToDate) {
                ensureToNotBeforeFrom()
                recomputeMonthDay()
                updateSubtotal()
            }
        }
    }

    private fun openDatePickerFor(target: TextView, onPicked: (() -> Unit)? = null) {
        val cal = Calendar.getInstance()
        runCatching { df.parse(target.text.toString()) }.onSuccess { if (it != null) cal.time = it }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, y1, m1, d1 ->
            cal.set(y1, m1, d1)
            target.text = df.format(cal.time)
            onPicked?.invoke()
        }, y, m, d).show()
    }

    private fun ensureToNotBeforeFrom() {
        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull()
        val to   = runCatching { df.parse(tvToDate.text.toString())   }.getOrNull()
        if (from != null && to != null && to.before(from)) {
            tvToDate.text = tvFromDate.text
        }
    }

    /* ---------------------- Tính chu kỳ & tiền ---------------------- */

    private fun recomputeMonthDay() {
        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull() ?: return
        val to   = runCatching { df.parse(tvToDate.text.toString())   }.getOrNull() ?: return

        val cFrom = Calendar.getInstance().apply { time = from }
        val cTo   = Calendar.getInstance().apply { time = to }
        if (cTo.before(cFrom)) cTo.time = cFrom.time

        var months = (cTo.get(Calendar.YEAR) - cFrom.get(Calendar.YEAR)) * 12 +
                (cTo.get(Calendar.MONTH) - cFrom.get(Calendar.MONTH))

        val days: Int
        if (cTo.get(Calendar.DAY_OF_MONTH) < cFrom.get(Calendar.DAY_OF_MONTH)) {
            months = max(0, months - 1)
            val tmp = Calendar.getInstance().apply {
                set(cTo.get(Calendar.YEAR), cTo.get(Calendar.MONTH), 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }
            val daysInPrevMonth = tmp.get(Calendar.DAY_OF_MONTH)
            days = (daysInPrevMonth - cFrom.get(Calendar.DAY_OF_MONTH)) + cTo.get(Calendar.DAY_OF_MONTH)
        } else {
            days = cTo.get(Calendar.DAY_OF_MONTH) - cFrom.get(Calendar.DAY_OF_MONTH)
        }
        tvMonthDay.text = "${months} tháng, ${days} ngày"
    }

    private fun updateAmountLabels() {
        tvAmountPerMonth.text = "Thành tiền: ${vn.format(baseRent)} đ / tháng"
    }

    private fun updateSubtotal() {
        // Tính tiền theo số tháng + ngày lẻ (tạm quy đổi 30 ngày = 1 tháng)
        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull() ?: return
        val to   = runCatching { df.parse(tvToDate.text.toString())   }.getOrNull() ?: return

        val cFrom = Calendar.getInstance().apply { time = from }
        val cTo   = Calendar.getInstance().apply { time = to }
        if (cTo.before(cFrom)) cTo.time = cFrom.time

        var months = (cTo.get(Calendar.YEAR) - cFrom.get(Calendar.YEAR)) * 12 +
                (cTo.get(Calendar.MONTH) - cFrom.get(Calendar.MONTH))

        val days: Int
        if (cTo.get(Calendar.DAY_OF_MONTH) < cFrom.get(Calendar.DAY_OF_MONTH)) {
            months = max(0, months - 1)
            val tmp = Calendar.getInstance().apply {
                set(cTo.get(Calendar.YEAR), cTo.get(Calendar.MONTH), 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }
            val dimPrev = tmp.get(Calendar.DAY_OF_MONTH)
            days = (dimPrev - cFrom.get(Calendar.DAY_OF_MONTH)) + cTo.get(Calendar.DAY_OF_MONTH)
        } else {
            days = cTo.get(Calendar.DAY_OF_MONTH) - cFrom.get(Calendar.DAY_OF_MONTH)
        }

        val subtotal = (months * baseRent) + ((days / 30.0) * baseRent).roundToInt()
        tvSubTotal.text = "Thành tiền ${vn.format(subtotal)} đ"
    }

    /* ---------------------- DB helpers ---------------------- */

    private fun queryRoomName(roomId: Int): String? {
        if (roomId <= 0) return null
        val db = DatabaseHelper(this).readableDatabase
        db.rawQuery("SELECT name FROM rooms WHERE id=?", arrayOf(roomId.toString())).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun queryRoomBaseRent(roomId: Int): Int {
        if (roomId <= 0) return 0
        val db = DatabaseHelper(this).readableDatabase
        db.rawQuery("SELECT baseRent FROM rooms WHERE id=?", arrayOf(roomId.toString())).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    /* ---------------------- Insert invoice ---------------------- */

    private fun createInvoice() {
        if (roomId <= 0) {
            Toast.makeText(this, "Thiếu thông tin phòng!", Toast.LENGTH_SHORT).show()
            return
        }

        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull()
        val to   = runCatching { df.parse(tvToDate.text.toString())   }.getOrNull()
        val due  = runCatching { df.parse(tvDueDate.text.toString())  }.getOrNull()
        if (from == null || to == null || due == null) {
            Toast.makeText(this, "Ngày chưa hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }

        // Lấy subtotal đã hiển thị
        val subtotalText = tvSubTotal.text?.toString() ?: ""
        val subtotal = Regex("""(\d[\d\.]*)""").find(subtotalText)?.value?.replace(".", "")?.toIntOrNull()
            ?: baseRent

        // period lấy theo "tháng của fromDate"
        val cal = Calendar.getInstance().apply { time = from }
        val periodYear  = cal.get(Calendar.YEAR)
        val periodMonth = cal.get(Calendar.MONTH) + 1

        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("roomId", roomId)
            put("periodYear", periodYear)
            put("periodMonth", periodMonth)
            put("roomRent", baseRent)
            put("electricKwh", 0)
            put("waterM3", 0)
            put("serviceFee", 0)
            put("totalAmount", subtotal)
            put("paid", 0) // 0 = chưa thu
            put("createdAt", now)
        }

        val db = DatabaseHelper(this).writableDatabase
        val rowId = db.insert("invoices", null, values)

        if (rowId > 0) {
            Toast.makeText(this, "Đã lập hóa đơn #$rowId cho $roomName", Toast.LENGTH_SHORT).show()
            finish() // quay lại danh sách hoặc màn trước
        } else {
            Toast.makeText(this, "Lập hóa đơn thất bại!", Toast.LENGTH_SHORT).show()
        }
    }
}
