package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var spMonth: Spinner
    private lateinit var spStatus: Spinner
    private lateinit var rv: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progress: ProgressBar

    private lateinit var listAdapter: InvoiceListAdapter

    // state đang chọn
    private var selMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1..12
    private var selYear  = Calendar.getInstance().get(Calendar.YEAR)
    private var selStatus: StatusFilter = StatusFilter.ALL

    private val vn: NumberFormat by lazy { NumberFormat.getInstance(Locale("vi", "VN")) }
    private val df: SimpleDateFormat by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invoice_list)

        // window insets
        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, 0)
                insets
            }
        }

        // Header (nếu có)
        findViewById<TextView?>(R.id.tvHeaderTitle)?.text = getString(R.string.invoice_list_title)
        findViewById<TextView?>(R.id.tvHeaderSubtitle)?.text = getString(R.string.invoice_list_subtitle)
        findViewById<View?>(R.id.ivBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Views
        spMonth   = findViewById(R.id.spMonth)
        spStatus  = findViewById(R.id.spStatus)
        rv        = findViewById(R.id.rvInvoices)
        emptyView = findViewById(R.id.tvEmpty)
        progress  = findViewById(R.id.progress)

        // RecyclerView
        listAdapter = InvoiceListAdapter(
            onItemClick = { /* TODO: mở chi tiết hóa đơn */ },
            onMoreClick = { _, _ -> /* TODO: popup Sửa/Xóa/Chia sẻ */ }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = listAdapter

        // Month picker (spinner-giả)
        initMonthPickerLikeSpinner()

        // Status filter
        initStatusSpinner()

        // Load đầu tiên
        reload()
    }

    /* ------------------------ Pickers & Filters ------------------------ */

    private fun initMonthPickerLikeSpinner() {
        val curText = monthText(selMonth - 1, selYear)
        spMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf(curText))

        spMonth.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_UP) { showMonthPicker(); true } else false
        }
        spMonth.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
            ) { showMonthPicker(); true } else false
        }
        (spMonth.parent as? View)?.setOnClickListener { showMonthPicker() }
    }

    private fun initStatusSpinner() {
        // Thứ tự phải khớp enum StatusFilter
        val labels = listOf("Tất cả", "Chưa thu", "Đã thu", "Hủy")
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spStatus.setSelection(StatusFilter.ALL.ordinal)
        spStatus.setOnItemSelectedListener(SimpleItemSelected {
            selStatus = StatusFilter.values()[spStatus.selectedItemPosition]
            reload()
        })
    }

    private fun showMonthPicker() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selYear)
            set(Calendar.MONTH, selMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)

        val dialog = DatePickerDialog(
            this,
            { _, yPicked, mPicked, _ ->
                selYear = yPicked
                selMonth = mPicked + 1
                spMonth.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(monthText(mPicked, yPicked))
                )
                reload()
            },
            y, m, cal.get(Calendar.DAY_OF_MONTH)
        )
        // Ẩn chọn ngày
        try {
            val dayId = resources.getIdentifier("day", "id", "android")
            dialog.datePicker.findViewById<View>(dayId)?.visibility = View.GONE
        } catch (_: Exception) { }
        dialog.setTitle(getString(R.string.pick_month))
        dialog.show()
    }

    private fun monthText(month0Based: Int, year: Int): String =
        String.format(Locale.getDefault(), "Tháng %d/%d", month0Based + 1, year)

    /* ------------------------ Loading ------------------------ */

    private fun reload() {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val data = queryInvoices(periodMonth = selMonth, periodYear = selYear, filter = selStatus)
            withContext(Dispatchers.Main) {
                showLoading(false)
                listAdapter.submitList(data)
                emptyView.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                emptyView.text = getString(R.string.invoice_list_empty, selMonth, selYear)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        rv.alpha = if (show) 0.4f else 1f
    }

    /* ------------------------ DB Query ------------------------ */

    private fun queryInvoices(periodMonth: Int, periodYear: Int, filter: StatusFilter): List<InvoiceCardItem> {
        val db = DatabaseHelper(this).readableDatabase

        // Base SQL
        val base = StringBuilder(
            """
            SELECT inv.id, r.name, inv.totalAmount, inv.roomRent, inv.paid, inv.createdAt
            FROM invoices inv
            JOIN rooms r ON r.id = inv.roomId
            WHERE inv.periodMonth = ? AND inv.periodYear = ?
            """.trimIndent()
        )

        val args = ArrayList<String>(3).apply {
            add(periodMonth.toString())
            add(periodYear.toString())
        }

        // Status filter
        when (filter) {
            StatusFilter.UNPAID -> { base.append(" AND inv.paid = 0 ") }
            StatusFilter.PAID   -> { base.append(" AND inv.paid = 1 ") }
            StatusFilter.CANCEL -> { base.append(" AND inv.paid = 2 ") } // ví dụ: 2 = Hủy (nếu bạn dùng mã khác, chỉnh lại)
            StatusFilter.ALL    -> { /* no-op */ }
        }

        base.append(" ORDER BY inv.createdAt DESC ")

        val list = mutableListOf<InvoiceCardItem>()
        db.rawQuery(base.toString(), args.toTypedArray()).use { cur ->
            while (cur.moveToNext()) {
                val invoiceId = cur.getInt(0).toString()
                val roomName  = cur.getString(1)
                val total     = cur.getInt(2)
                val rent      = cur.getInt(3)
                val paidCode  = cur.getInt(4)        // 0/1/(2=hủy) tuỳ DB bạn
                val createdAt = cur.getLong(5)

                val mainStatus = when (paidCode) {
                    1    -> "Đã thu"
                    2    -> "Hủy"
                    else -> "Chưa thu"
                }

                list.add(
                    InvoiceCardItem(
                        id          = invoiceId,
                        title       = roomName,
                        mainStatus  = mainStatus,
                        rent        = "${vn.format(total)}đ",
                        deposit     = "${vn.format(rent)}đ",
                        collected   = if (paidCode == 1) "Đã thu ${vn.format(total)}đ" else "Chưa thu",
                        createdDate = df.format(Date(createdAt)),
                        moveInDate  = "—",
                        endDate     = "—"
                    )
                )
            }
        }
        return list
    }

    /* ------------------------ Helpers ------------------------ */

    private enum class StatusFilter { ALL, UNPAID, PAID, CANCEL }

    /**
     * OnItemSelectedListener rút gọn cho Spinner
     */
    private class SimpleItemSelected(
        val onSelected: () -> Unit
    ) : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) = onSelected()
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
