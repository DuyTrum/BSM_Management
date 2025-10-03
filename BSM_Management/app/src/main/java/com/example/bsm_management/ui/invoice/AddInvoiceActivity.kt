package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class AddInvoiceActivity : AppCompatActivity() {

    private lateinit var spReason: Spinner
    private lateinit var tvDueDate: TextView
    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var tvMonthDay: TextView

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

        // Insets cho root id=main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        spReason = findViewById(R.id.spReason)
        tvDueDate = findViewById(R.id.tvDueDate)
        tvFromDate = findViewById(R.id.tvFromDate)
        tvToDate = findViewById(R.id.tvToDate)
        tvMonthDay = findViewById(R.id.tvMonthDay)

        setupReasonSpinnerAsDialog()
        setupDatePickers()
        recomputeMonthDay()
        setupHeader(
            title = "Lập hóa đơn mới",
            subtitle = "Điền thông tin và chọn ngày"
        )
    }

    private fun setupHeader(title: String, subtitle: String) {
        val header: View = findViewById(R.id.headerBack)
        val tvTitle = header.findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = header.findViewById<TextView>(R.id.tvHeaderSubtitle)
        val btnBack = header.findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = title
        tvSubtitle.text = subtitle
        btnBack.setOnClickListener { finish() }
    }

    private fun setupReasonSpinnerAsDialog() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spReason.adapter = adapter
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

    private fun setupDatePickers() {
        tvDueDate.setOnClickListener { openDatePickerFor(tvDueDate) }
        tvFromDate.setOnClickListener {
            openDatePickerFor(tvFromDate) {
                ensureToNotBeforeFrom()
                recomputeMonthDay()
            }
        }
        tvToDate.setOnClickListener {
            openDatePickerFor(tvToDate) {
                ensureToNotBeforeFrom()
                recomputeMonthDay()
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
        val to = runCatching { df.parse(tvToDate.text.toString()) }.getOrNull()
        if (from != null && to != null && to.before(from)) {
            tvToDate.text = tvFromDate.text
        }
    }

    private fun recomputeMonthDay() {
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
            val daysInPrevMonth = tmp.get(Calendar.DAY_OF_MONTH)
            days = (daysInPrevMonth - cFrom.get(Calendar.DAY_OF_MONTH)) + cTo.get(Calendar.DAY_OF_MONTH)
        } else {
            days = cTo.get(Calendar.DAY_OF_MONTH) - cFrom.get(Calendar.DAY_OF_MONTH)
        }
        tvMonthDay.text = "${months} tháng, ${days} ngày"
    }
}
