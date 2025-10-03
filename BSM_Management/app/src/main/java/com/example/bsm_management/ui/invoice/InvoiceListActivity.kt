package com.example.bsm_management.ui.invoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import java.util.Calendar
import java.util.Locale

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var spMonth: Spinner
    private lateinit var spStatus: Spinner
    private lateinit var rv: RecyclerView
    private lateinit var listAdapter: InvoiceListAdapter   // <-- dùng adapter mới

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invoice_list)

        // Fit system bars cho root (nếu layout có id=main)
        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, 0)
                insets
            }
        }

        // View refs
        spMonth = findViewById(R.id.spMonth)
        spStatus = findViewById(R.id.spStatus)
        rv = findViewById(R.id.rvInvoices)

        // Month picker (giả Spinner)
        initMonthPickerLikeSpinner()

        // Spinner trạng thái
        val statuses = listOf(
            "Chưa thu tiền (Khách nợ tiền thuê)",
            "Đã thu",
            "Hủy"
        )
        spStatus.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        // RecyclerView + Adapter
        listAdapter = InvoiceListAdapter(
            onItemClick = { /* TODO: mở chi tiết hóa đơn */ },
            onMoreClick = { _, _ -> /* TODO: popup Sửa/Xóa/Share sau này */ }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = listAdapter

        // Mock data: KHỚP VỚI InvoiceCardItem (id, title, mainStatus, rent, deposit, collected, createdDate, moveInDate, endDate)
        listAdapter.submitList(
            listOf(
                InvoiceCardItem(
                    id = "INV_001",
                    title = "Phòng 1 - #45600",
                    mainStatus = "Trong thời hạn hợp đồng",
                    rent = "3.000.000đ",
                    deposit = "3.000.000đ",
                    collected = "Chưa thu",
                    createdDate = "07/09/2025",
                    moveInDate = "07/09/2025",
                    endDate = "Vô thời hạn"
                ),
                InvoiceCardItem(
                    id = "INV_002",
                    title = "Phòng 2 - #45601",
                    mainStatus = "Trong thời hạn hợp đồng",
                    rent = "3.500.000đ",
                    deposit = "3.500.000đ",
                    collected = "Đã thu 3.500.000đ",
                    createdDate = "10/09/2025",
                    moveInDate = "15/09/2025",
                    endDate = "10/09/2026"
                )
            )
        )
    }

    /** Spinner giả: chạm mở DatePickerDialog nhưng chỉ lấy THÁNG/NĂM */
    private fun initMonthPickerLikeSpinner() {
        val cal = Calendar.getInstance()
        val curText = monthText(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        spMonth.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf(curText))

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

    private fun showMonthPicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        val dialog = DatePickerDialog(
            this,
            { _, y, m, _ ->
                val txt = monthText(m, y)
                spMonth.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(txt)
                )
                // TODO: filter lại list theo (m, y)
            },
            year, month, cal.get(Calendar.DAY_OF_MONTH)
        )

        // Ẩn chọn ngày
        try {
            val dayId = resources.getIdentifier("day", "id", "android")
            dialog.datePicker.findViewById<View>(dayId)?.visibility = View.GONE
        } catch (_: Exception) { }

        dialog.setTitle("Chọn tháng")
        dialog.show()
    }

    private fun monthText(month0Based: Int, year: Int): String =
        String.format(Locale.getDefault(), "Tháng %d/%d", month0Based + 1, year)
}
