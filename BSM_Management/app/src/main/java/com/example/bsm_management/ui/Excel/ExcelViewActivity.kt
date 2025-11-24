package com.example.bsm_management.ui.excel

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import org.apache.poi.ss.usermodel.WorkbookFactory


class ExcelViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excel_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootExcelViewer)) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.containerExcel)

        val fileUri = intent.getStringExtra("fileUri") ?: return
        val file = contentResolver.openInputStream(android.net.Uri.parse(fileUri)) ?: return

        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)

        // Header
        val headers = listOf("Tên khách", "SĐT", "CCCD", "Địa chỉ", "Phòng")
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        headers.forEach {
            headerRow.addView(createCell(it, bold = true, bg = "#EEEEEE"))
        }

        container.addView(headerRow)

        // Rows (bỏ dòng đầu tiên vì là header)
        for (i in 1..sheet.lastRowNum) {
            val excelRow = sheet.getRow(i) ?: continue

            val name = excelRow.getCell(0)?.toString() ?: ""
            val phone = excelRow.getCell(1)?.toString() ?: ""
            val cccd = excelRow.getCell(2)?.toString() ?: ""
            val address = excelRow.getCell(3)?.toString() ?: ""
            val room = excelRow.getCell(4)?.toString() ?: ""

            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            listOf(name, phone, cccd, address, room).forEach {
                rowLayout.addView(createCell(it))
            }

            container.addView(rowLayout)
        }
    }

    private fun createCell(
        text: String,
        bold: Boolean = false,
        bg: String = "#FFFFFF"
    ): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(24, 16, 24, 16)
        tv.gravity = Gravity.CENTER
        tv.width = dp(120)  // Cột thẳng hàng
        tv.setBackgroundColor(Color.parseColor(bg))
        tv.setTextColor(Color.BLACK)
        tv.setBackgroundResource(R.drawable.cell_border)

        if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        return tv
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
