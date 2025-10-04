// com.example.bsm_management.ui.hostel.AddHostelActivity.kt
package com.example.bsm_management.ui.hostel

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import database.DatabaseHelper

class AddHostelActivity : AppCompatActivity() {

    private lateinit var edtSampleRoom: TextInputEditText
    private lateinit var edtArea: TextInputEditText
    private lateinit var edtPrice: TextInputEditText
    private lateinit var edtInvoiceDay: TextInputEditText
    private lateinit var edtDueDays: TextInputEditText
    private lateinit var ddlMaxPeople: AutoCompleteTextView
    private lateinit var switchAuto: MaterialSwitch
    private lateinit var btnClose: MaterialButton
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hostel) // đúng file layout bạn gửi

        findViewById<MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        edtSampleRoom = findViewById(R.id.edtSampleRoom)
        edtArea       = findViewById(R.id.edtArea)
        edtPrice      = findViewById(R.id.edtPrice)
        edtInvoiceDay = findViewById(R.id.edtInvoiceDay)
        edtDueDays    = findViewById(R.id.edtDueDays)
        ddlMaxPeople  = findViewById(R.id.ddlMaxPeople)
        switchAuto    = findViewById(R.id.switchAuto)
        btnClose      = findViewById(R.id.btnClose)
        btnNext       = findViewById(R.id.btnNext)

        // dropdown “Tối đa người ở / phòng”
        ddlMaxPeople.setAdapter(
            android.widget.ArrayAdapter(
                this, android.R.layout.simple_list_item_1,
                listOf("Không giới hạn","1","2","3","4","5","6","7","8")
            )
        )

        btnClose.setOnClickListener { finish() }
        btnNext.setOnClickListener { saveAndGo() }
    }

    private fun saveAndGo() {
        val auto = switchAuto.isChecked
        val count = edtSampleRoom.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val price = edtPrice.text?.toString()?.trim()?.toIntOrNull() ?: 0

        if (auto) {
            if (count <= 0) { toast("Nhập số lượng phòng mẫu > 0"); return }
            if (price <= 0) { toast("Nhập giá thuê mẫu > 0"); return }
            DatabaseHelper(this).insertRoomsAuto(count = count, baseRent = price, floor = 1)
        }

        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
