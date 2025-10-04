package com.example.bsm_management.ui.hostel

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R
import com.example.bsm_management.databinding.ActivityAddHostelBinding
import database.DatabaseHelper
import android.content.ContentValues

class AddHostelActivity : AppCompatActivity() {
    private lateinit var vb: ActivityAddHostelBinding
    private lateinit var db: DatabaseHelper
    private var rentMode: String = "ROOM" // ROOM or BED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityAddHostelBinding.inflate(layoutInflater)
        setContentView(vb.root)

        db = DatabaseHelper(this)

        // Topbar
        vb.topBar.setNavigationOnClickListener { finish() }

        // Dropdown max people
        vb.ddlMaxPeople.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, (1..10).map { "$it người" })
        )

        // Chọn “thuê theo phòng / giường”
        vb.cardByRoom.setOnClickListener {
            rentMode = "ROOM"
            vb.cardByRoom.strokeColor = getColor(R.color.bsm_green)
            vb.cardByBed.strokeColor = getColor(R.color.bsm_divider)
        }
        vb.cardByBed.setOnClickListener {
            rentMode = "BED"
            vb.cardByBed.strokeColor = getColor(R.color.bsm_green)
            vb.cardByRoom.strokeColor = getColor(R.color.bsm_divider)
        }

        vb.btnClose.setOnClickListener { finish() }
        vb.btnNext.setOnClickListener { saveAndNext() }
    }

    private fun saveAndNext() {
        val name = vb.edtName.text?.toString()?.trim().orEmpty()
        val sampleRooms = vb.edtSampleRoom.text?.toString()?.toIntOrNull() ?: 0
        val area = vb.edtArea.text?.toString()?.toIntOrNull() ?: 0
        val price = vb.edtPrice.text?.toString()?.toLongOrNull() ?: 0L
        val invoiceDay = vb.edtInvoiceDay.text?.toString()?.toIntOrNull() ?: 1
        val dueDays = vb.edtDueDays.text?.toString()?.toIntOrNull() ?: 5
        val auto = if (vb.switchAuto.isChecked) 1 else 0

        if (name.isEmpty()) {
            Toast.makeText(this, "Nhập tên nhà trọ", Toast.LENGTH_SHORT).show()
            return
        }

        val cv = ContentValues().apply {
            put("name", name)
            put("type", "HOSTEL")
            put("rentMode", rentMode)
            put("autoGenerate", auto)
            put("sampleRooms", sampleRooms)
            put("sampleArea", area)
            put("samplePrice", price)
            put("maxPeople", parseMaxPeople(vb.ddlMaxPeople.text?.toString()))
            put("invoiceDay", invoiceDay)
            put("dueDays", dueDays)
            put("createdAt", System.currentTimeMillis())
        }

        val id = db.writableDatabase.insert("hostels", null, cv)
        if (id > 0) {
            Toast.makeText(this, "Đã lưu nhà trọ #$id", Toast.LENGTH_SHORT).show()
            // TODO: chuyển bước tiếp theo hoặc finish()
            finish()
        } else {
            Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseMaxPeople(text: CharSequence?): Int {
        val t = text?.toString()?.trim().orEmpty()
        return t.split(" ").firstOrNull()?.toIntOrNull() ?: 0
    }
}
