package com.example.bsm_management.ui.invoice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var selMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selYear = Calendar.getInstance().get(Calendar.YEAR)
    private var selStatus: StatusFilter = StatusFilter.ALL

    private val vn by lazy { NumberFormat.getInstance(Locale("vi", "VN")) }
    private val df by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invoice_list)

        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, 0)
                insets
            }
        }

        // Header
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = getString(R.string.invoice_list_title)
        findViewById<TextView>(R.id.tvHeaderSubtitle)?.text = getString(R.string.invoice_list_subtitle)
        findViewById<View>(R.id.ivBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Views
        spMonth = findViewById(R.id.spMonth)
        spStatus = findViewById(R.id.spStatus)
        rv = findViewById(R.id.rvInvoices)
        emptyView = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progress)

        listAdapter = InvoiceListAdapter(
            onItemClick = { item ->
                item.id.toIntOrNull()?.let {
                    startActivity(Intent(this, InvoiceDetailActivity::class.java).putExtra("invoiceId", it))
                }
            },
            onMoreClick = { _, item ->
                item.id.toIntOrNull()?.let {
                    startActivity(Intent(this, InvoiceDetailActivity::class.java).putExtra("invoiceId", it))
                }
            },
            onCall = { phone -> dial(phone) }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = listAdapter

        initMonthPickerLikeSpinner()
        initStatusSpinner()
        reload()
    }

    private fun dial(phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(this, "Chưa có số điện thoại", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")))
    }

    /* ---------------- Filters ---------------- */
    private fun initMonthPickerLikeSpinner() {
        spMonth.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf(monthText(selMonth - 1, selYear))
        )
        spMonth.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_UP) showMonthPicker()
            true
        }
    }

    private fun initStatusSpinner() {
        val labels = listOf("Tất cả", "Chưa thu", "Đã thu", "Hủy")
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spStatus.setSelection(StatusFilter.ALL.ordinal)
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selStatus = StatusFilter.values()[position]; reload()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showMonthPicker() {
        val dialog = android.app.DatePickerDialog(
            this,
            { _, year, month, _ ->
                selYear = year
                selMonth = month + 1
                spMonth.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(monthText(selMonth - 1, selYear))
                )
                reload()
            },
            selYear, selMonth - 1, 1
        )
        dialog.setOnShowListener {
            val dp = dialog.datePicker
            runCatching {
                val dayId = resources.getIdentifier("day", "id", "android")
                dp.findViewById<View>(dayId)?.visibility = View.GONE
            }
        }
        dialog.show()
    }

    private fun monthText(month0: Int, year: Int) =
        String.format(Locale.getDefault(), "Tháng %d/%d", month0 + 1, year)

    /* ---------------- Load data ---------------- */
    private fun reload() {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val data = queryInvoices(selMonth, selYear, selStatus)
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

    /* ---------------- Query DB ---------------- */
    private fun queryInvoices(periodMonth: Int, periodYear: Int, filter: StatusFilter): List<InvoiceCardItem> {
        val db = DatabaseHelper(this).readableDatabase
        val sql = buildString {
            append(
                """
                SELECT inv.id,
                       r.name AS roomName,
                       inv.totalAmount, inv.roomRent, inv.paid,
                       inv.createdAt, inv.dueAt, inv.reason,
                       c.tenantPhone,
                       inv.periodMonth, inv.periodYear
                FROM invoices inv
                JOIN rooms r ON r.id = inv.roomId
                LEFT JOIN contracts c ON c.roomId = inv.roomId AND c.active = 1
                WHERE inv.periodMonth = ? AND inv.periodYear = ?
            """.trimIndent()
            )
            when (filter) {
                StatusFilter.UNPAID -> append(" AND inv.paid = 0 ")
                StatusFilter.PAID -> append(" AND inv.paid = 1 ")
                StatusFilter.CANCEL -> append(" AND inv.paid = 2 ")
                StatusFilter.ALL -> {}
            }
            append(" ORDER BY inv.createdAt DESC ")
        }

        val args = arrayOf(periodMonth.toString(), periodYear.toString())
        val list = mutableListOf<InvoiceCardItem>()
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                val invoiceId = c.getInt(0).toString()
                val roomName = c.getString(1) ?: ""
                val total = c.getInt(2)
                val paidCode = c.getInt(4)
                val createdAtMs = c.getLong(5)
                val dueAtMs = c.getLong(6)
                val reasonTxt = c.getString(7)
                val phone = c.getString(8) ?: ""
                val periodMonthDb = c.getInt(9)
                val periodYearDb = c.getInt(10)

                val mainStatus = when (paidCode) {
                    1 -> "Đã thu"
                    2 -> "Hủy"
                    else -> "Chưa thu"
                }

                val createdStr = if (createdAtMs > 0) df.format(Date(createdAtMs)) else "—"
                val dueStr = if (dueAtMs > 0) df.format(Date(dueAtMs)) else "—"
                val reasonStr = if (reasonTxt.isNullOrBlank()) "—" else reasonTxt.trim()
                val remainStr = if (paidCode == 1) "0đ" else "${vn.format(total)}đ"
                val collectedStr = if (paidCode == 1) "Đã thu ${vn.format(total)}đ" else "Chưa thu"

                list.add(
                    InvoiceCardItem(
                        id = invoiceId,
                        title = roomName,
                        mainStatus = mainStatus,
                        rent = "${vn.format(total)}đ",
                        deposit = remainStr,
                        collected = collectedStr,
                        createdDate = createdStr,
                        moveInDate = reasonStr,
                        endDate = dueStr,
                        phone = phone,
                        periodMonth = periodMonthDb,
                        periodYear = periodYearDb
                    )
                )
            }
        }
        return list
    }

    private enum class StatusFilter { ALL, UNPAID, PAID, CANCEL }
}
