package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

class AddInvoiceActivity : AppCompatActivity() {

    // View
    private lateinit var spReason: Spinner
    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var tvSubTotal: TextView
    private lateinit var btnCreate: Button
    private lateinit var edtElectricQty: EditText
    private lateinit var edtElectricRate: EditText
    private lateinit var edtWaterQty: EditText
    private lateinit var edtWaterRate: EditText
    private lateinit var edtService: EditText

    // Data
    private var roomId: Int = -1
    private var roomName: String? = null
    private var baseRent: Int = 0

    // Format
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val vn = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))

    // Prefs
    private lateinit var prefs: SharedPreferences

    private val reasons = arrayOf(
        "Thu tiền hàng tháng",
        "Thu tiền cọc",
        "Hoàn tiền cọc",
        "Thu tiền kết thúc hợp đồng"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_invoice)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // SharedPreferences
        prefs = getSharedPreferences("invoice_prefs", MODE_PRIVATE)

        // Nhận dữ liệu
        roomId = intent.getIntExtra("roomId", -1)
        roomName = intent.getStringExtra("roomName")

        // Ánh xạ View
        spReason = findViewById(R.id.spReason)
        tvFromDate = findViewById(R.id.tvFromDate)
        tvToDate = findViewById(R.id.tvToDate)
        tvSubTotal = findViewById(R.id.tvSubTotal)
        btnCreate = findViewById(R.id.btnCreateInvoice)
        edtElectricQty = findViewById(R.id.edtElectricQty)
        edtElectricRate = findViewById(R.id.edtElectricRate)
        edtWaterQty = findViewById(R.id.edtWaterQty)
        edtWaterRate = findViewById(R.id.edtWaterRate)
        edtService = findViewById(R.id.edtService)

        // Header
        setupHeader(
            title = "Lập hóa đơn: ${roomName ?: queryRoomName(roomId) ?: "Phòng ?"}",
            subtitle = "Điền thông tin và chọn ngày"
        )

        setupSectionTitles()
        baseRent = queryRoomBaseRent(roomId)
        setupReasonSpinnerAsDialog()
        setupDatePickers()
        setupDefaultDates()
        loadLastRates() // 🔹 đọc đơn giá gần nhất từ SharedPreferences

        updateSubtotal()
        setupAutoRecalculate()

        btnCreate.setOnClickListener { createInvoice() }
    }

    /* ---------------- HEADER ---------------- */
    private fun setupHeader(title: String, subtitle: String) {
        val header: View = findViewById(R.id.headerBack)
        val tvTitle = header.findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSub = header.findViewById<TextView>(R.id.tvHeaderSubtitle)
        val btnBack = header.findViewById<ImageView>(R.id.ivBack)
        tvTitle.text = title
        tvSub.text = subtitle
        btnBack.setOnClickListener { finish() }
    }

    private fun setupSectionTitles() {
        findViewById<View>(R.id.secReason).apply {
            findViewById<TextView>(R.id.tvTitle).text = "Lý do lập hóa đơn"
            findViewById<TextView>(R.id.tvSubtitle).text = "Chọn loại hóa đơn cần lập"
        }
        findViewById<View>(R.id.secFirstMonth).apply {
            findViewById<TextView>(R.id.tvTitle).text = "Kỳ tính tiền"
            findViewById<TextView>(R.id.tvSubtitle).text = "Chọn khoảng ngày tính tiền thuê"
        }
        findViewById<View>(R.id.secExtraCosts).apply {
            findViewById<TextView>(R.id.tvTitle).text = "Các khoản chi tiêu khác"
            findViewById<TextView>(R.id.tvSubtitle).text = "Điện, nước, dịch vụ..."
        }
    }

    /* ---------------- SPINNER ---------------- */
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
        spReason.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showReasonDialog() }
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

    /* ---------------- DATE PICKER ---------------- */
    private fun setupDatePickers() {
        tvFromDate.setOnClickListener {
            openDatePickerFor(tvFromDate) {
                ensureToNotBeforeFrom()
                updateSubtotal()
            }
        }
        tvToDate.setOnClickListener {
            openDatePickerFor(tvToDate) {
                ensureToNotBeforeFrom()
                updateSubtotal()
            }
        }
    }

    private fun setupDefaultDates() {
        val now = Calendar.getInstance()
        val firstDay = now.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        tvFromDate.text = df.format(firstDay.time)

        val lastDay = now.clone() as Calendar
        lastDay.set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH))
        tvToDate.text = df.format(lastDay.time)
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
        val to = runCatching { df.parse(tvToDate.text.toString()) }.getOrNull()
        if (from != null && to != null && to.before(from)) {
            tvToDate.text = tvFromDate.text
        }
    }

    /* ---------------- TÍNH TIỀN ---------------- */
    private fun updateSubtotal() {
        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull() ?: return
        val to = runCatching { df.parse(tvToDate.text.toString()) }.getOrNull() ?: return

        val cFrom = Calendar.getInstance().apply { time = from }
        val cTo = Calendar.getInstance().apply { time = to }
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

        val sameMonthFull =
            (cFrom.get(Calendar.YEAR) == cTo.get(Calendar.YEAR)) &&
                    (cFrom.get(Calendar.MONTH) == cTo.get(Calendar.MONTH)) &&
                    (cFrom.get(Calendar.DAY_OF_MONTH) == 1) &&
                    (cTo.get(Calendar.DAY_OF_MONTH) == cTo.getActualMaximum(Calendar.DAY_OF_MONTH))

        val roomSubtotal = if (sameMonthFull) baseRent
        else (months * baseRent) + ((days / 30.0) * baseRent).roundToInt()

        // Tính điện, nước, dịch vụ
        val electricRate = edtElectricRate.text.toString().toIntOrNull() ?: 0
        val electricQty = edtElectricQty.text.toString().toIntOrNull() ?: 0
        val waterRate = edtWaterRate.text.toString().toIntOrNull() ?: 0
        val waterQty = edtWaterQty.text.toString().toIntOrNull() ?: 0
        val service = edtService.text.toString().toIntOrNull() ?: 0

        val electricTotal = electricRate * electricQty
        val waterTotal = waterRate * waterQty

        val total = roomSubtotal + electricTotal + waterTotal + service
        tvSubTotal.text = "Thành tiền ${vn.format(total)} đ"
    }

    private fun setupAutoRecalculate() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSubtotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        edtElectricQty.addTextChangedListener(watcher)
        edtElectricRate.addTextChangedListener(watcher)
        edtWaterQty.addTextChangedListener(watcher)
        edtWaterRate.addTextChangedListener(watcher)
        edtService.addTextChangedListener(watcher)
    }

    /* ---------------- DB ---------------- */
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

    /* ---------------- LƯU & GỢI Ý ---------------- */
    private fun loadLastRates() {
        val lastElectricRate = prefs.getInt("lastElectricRate", 3500)
        val lastWaterRate = prefs.getInt("lastWaterRate", 8000)
        edtElectricRate.setText(lastElectricRate.toString())
        edtWaterRate.setText(lastWaterRate.toString())
    }

    private fun saveLastRates(electricRate: Int, waterRate: Int) {
        prefs.edit()
            .putInt("lastElectricRate", electricRate)
            .putInt("lastWaterRate", waterRate)
            .apply()
    }

    /* ---------------- TẠO HÓA ĐƠN ---------------- */
    private fun createInvoice() {
        if (roomId <= 0) {
            Toast.makeText(this, "Thiếu thông tin phòng!", Toast.LENGTH_SHORT).show()
            return
        }

        val from = runCatching { df.parse(tvFromDate.text.toString()) }.getOrNull()
        val to = runCatching { df.parse(tvToDate.text.toString()) }.getOrNull()
        if (from == null || to == null) {
            Toast.makeText(this, "Ngày chưa hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }

        val electricQty = edtElectricQty.text.toString().toIntOrNull() ?: 0
        val electricRate = edtElectricRate.text.toString().toIntOrNull() ?: 0
        val waterQty = edtWaterQty.text.toString().toIntOrNull() ?: 0
        val waterRate = edtWaterRate.text.toString().toIntOrNull() ?: 0
        val service = edtService.text.toString().toIntOrNull() ?: 0

        saveLastRates(electricRate, waterRate) // 🔹 lưu đơn giá mới

        val subtotalText = tvSubTotal.text?.toString() ?: ""
        val subtotal = Regex("""(\d[\d\.]*)""")
            .find(subtotalText)?.value?.replace(".", "")?.toIntOrNull()
            ?: baseRent

        val cal = Calendar.getInstance().apply { time = from }
        val periodYear = cal.get(Calendar.YEAR)
        val periodMonth = cal.get(Calendar.MONTH) + 1
        val now = System.currentTimeMillis()
        val dueAtForTest = now + 5_000

        val values = ContentValues().apply {
            put("roomId", roomId)
            put("periodYear", periodYear)
            put("periodMonth", periodMonth)
            put("roomRent", baseRent)
            put("electricKwh", electricQty)
            put("waterM3", waterQty)
            put("serviceFee", service)
            put("totalAmount", subtotal)
            put("paid", 0)
            put("createdAt", now)
            put("dueAt", dueAtForTest)
            put("reason", spReason.selectedItem.toString())
        }

        val db = DatabaseHelper(this).writableDatabase
        val rowId = db.insert("invoices", null, values)

        if (rowId > 0) {
            com.example.bsm_management.bg.ReminderScheduler.scheduleDueReminder(
                ctx = this,
                invoiceId = rowId.toInt(),
                roomName = roomName ?: "Không rõ",
                dueAt = dueAtForTest
            )
            Toast.makeText(this, "Đã lập hóa đơn #$rowId (nhắc sau 5s)", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Lập hóa đơn thất bại!", Toast.LENGTH_SHORT).show()
        }
    }
}
