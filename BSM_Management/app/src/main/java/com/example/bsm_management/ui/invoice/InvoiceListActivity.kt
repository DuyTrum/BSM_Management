package com.example.bsm_management.ui.invoice

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream
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

        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val btnBack = findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = "Danh sách hóa đơn"
        tvSubtitle.text = "Xem và quản lý tất cả hóa đơn theo tháng"
        btnBack.setOnClickListener { finish() }

        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, 0)
                insets
            }
        }

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
            onCall = { phone -> dial(phone) },
            onSend = { item -> sendInvoice(item) }
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
                selStatus = StatusFilter.values()[position]
                reload()
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
        "Tháng ${month0 + 1}/$year"

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

    private fun queryInvoices(month: Int, year: Int, filter: StatusFilter): List<InvoiceCardItem> {
        val db = DatabaseHelper(this).readableDatabase

        val sql = buildString {
            append("""
                SELECT inv.id,
                       r.name,
                       inv.totalAmount,
                       inv.roomRent,
                       inv.paid,
                       inv.createdAt,
                       inv.dueAt,
                       inv.reason,
                       c.tenantPhone,
                       c.tenantName,
                       inv.periodMonth,
                       inv.periodYear
                FROM invoices inv
                JOIN rooms r ON r.id = inv.roomId
                LEFT JOIN contracts c ON c.roomId = inv.roomId AND c.active = 1
                WHERE inv.periodMonth = ? AND inv.periodYear = ?
            """.trimIndent())

            when (filter) {
                StatusFilter.UNPAID -> append(" AND inv.paid = 0")
                StatusFilter.PAID -> append(" AND inv.paid = 1")
                StatusFilter.CANCEL -> append(" AND inv.paid = 2")
                StatusFilter.ALL -> {}
            }

            append(" ORDER BY inv.createdAt DESC")
        }

        val args = arrayOf(month.toString(), year.toString())
        val list = mutableListOf<InvoiceCardItem>()

        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                val paidCode = c.getInt(4)
                val mainStatus = when (paidCode) {
                    1 -> "Đã thu"
                    2 -> "Hủy"
                    else -> "Chưa thu"
                }

                list.add(
                    InvoiceCardItem(
                        id = c.getInt(0).toString(),
                        title = c.getString(1),
                        mainStatus = mainStatus,
                        rent = "${vn.format(c.getInt(2))}đ",
                        deposit = if (paidCode == 1) "0đ" else "${vn.format(c.getInt(2))}đ",
                        collected = if (paidCode == 1) "Đã thu ${vn.format(c.getInt(2))}đ" else "Chưa thu",
                        createdDate = df.format(Date(c.getLong(5))),
                        endDate = df.format(Date(c.getLong(6))),
                        moveInDate = c.getString(7) ?: "—",
                        phone = c.getString(8) ?: "",
                        tenantName = c.getString(9) ?: "",
                        periodMonth = c.getInt(10),
                        periodYear = c.getInt(11)
                    )
                )
            }
        }
        return list
    }

    private fun inflateInvoiceView(item: InvoiceCardItem): View {
        val view = layoutInflater.inflate(R.layout.invoice_export_view, null)

        view.findViewById<TextView>(R.id.tvRoom).text = item.title
        view.findViewById<TextView>(R.id.tvPeriod).text = "T.${item.periodMonth}, ${item.periodYear}"
        view.findViewById<TextView>(R.id.tvCreatedDate).text = item.createdDate
        view.findViewById<TextView>(R.id.tvDueDate).text = item.endDate
        view.findViewById<TextView>(R.id.tvTenantName).text = item.tenantName
        view.findViewById<TextView>(R.id.tvTenantPhone).text = "SĐT: ${item.phone}"
        view.findViewById<TextView>(R.id.tvReason).text = item.moveInDate
        view.findViewById<TextView>(R.id.tvRentAmount).text = item.rent
        view.findViewById<TextView>(R.id.tvDepositAmount).text = item.deposit
        view.findViewById<TextView>(R.id.tvTimes).text = "1 lần"
        view.findViewById<TextView>(R.id.tvTotalPaid).text = item.collected
        view.findViewById<TextView>(R.id.tvNote).text =
            "* Chú ý: Vui lòng thanh toán đúng hạn và trước ngày ${item.endDate}"

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

    private fun saveBitmapFile(bmp: Bitmap, fileName: String): File {
        val file = File(cacheDir, "$fileName.png")
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    private fun shareInvoiceImage(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(share, "Chia sẻ hóa đơn"))
    }

    private fun sendInvoice(item: InvoiceCardItem) {

        // 1) Inflate layout hóa đơn
        val view = inflateInvoiceView(item)

        // 2) Render bitmap
        val bmp = renderBitmap(view)

        // 3) Lưu file vào cache
        val file = saveBitmapFile(bmp, "invoice_${item.id}")

        // 4) Share trực tiếp (không Zalo)
        shareInvoiceImage(file)
    }


    private enum class StatusFilter { ALL, UNPAID, PAID, CANCEL }
}
