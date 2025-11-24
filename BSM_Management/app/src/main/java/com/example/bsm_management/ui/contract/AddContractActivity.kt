package com.example.bsm_management.ui.contract

import android.R.attr.mode
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import database.dao.ContractDAO
import java.text.SimpleDateFormat
import java.util.*

class AddContractActivity : AppCompatActivity() {

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var dao: ContractDAO
    private var roomId = 0
    private var roomName = ""
    private var startDate: Long = 0
    private var endDate: Long? = null
    private var mode: String? = null
    private var contractId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contract)

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        dao = ContractDAO(this)
        roomId = intent.getIntExtra("roomId", 0)
        roomName = intent.getStringExtra("roomName") ?: "Không xác định"
        mode = intent.getStringExtra("mode")
        contractId = intent.getIntExtra("contractId", 0)

        setupHeader()
        setupLabels()
        setupThoiHanSpinner()
        setupDatePickers()
        setupSaveButton()

        // Mặc định ngày bắt đầu & kết thúc 6 tháng sau
        setDefaultDates()

        // Nếu là chế độ gia hạn => nạp sẵn dữ liệu cũ
        if (mode == "renew" && contractId > 0) preloadContractData(contractId)
    }

    /** ---------------- HEADER ---------------- */
    private fun setupHeader() {
        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = if (mode == "renew") "Gia hạn hợp đồng" else "Lập hợp đồng mới"
        tvSubtitle.text = "Phòng $roomName"
        ivBack.setOnClickListener { finish() }
    }

    /** ---------------- NẠP DỮ LIỆU CŨ (KHI GIA HẠN) ---------------- */
    private fun preloadContractData(contractId: Int) {
        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery(
            "SELECT * FROM contracts WHERE id=?",
            arrayOf(contractId.toString())
        )

        if (cursor.moveToFirst()) {
            findViewById<EditText>(R.id.edtTenKhach)
                .setText(cursor.getString(cursor.getColumnIndexOrThrow("tenantName")))
            findViewById<EditText>(R.id.edtSdtKhach)
                .setText(cursor.getString(cursor.getColumnIndexOrThrow("tenantPhone")))
            findViewById<EditText>(R.id.edtMucCoc)
                .setText(cursor.getInt(cursor.getColumnIndexOrThrow("deposit")).toString())

            startDate = cursor.getLong(cursor.getColumnIndexOrThrow("startDate"))
            endDate = cursor.getLong(cursor.getColumnIndexOrThrow("endDate"))

            val viewNgayVao = findViewById<View>(R.id.viewNgayVao1)
            val viewNgayKt = findViewById<View>(R.id.viewNgayKetThuc1)
            viewNgayVao.findViewById<TextView>(R.id.txtValue).text = df.format(Date(startDate))
            viewNgayKt.findViewById<TextView>(R.id.txtValue).text =
                if (endDate != null && endDate!! > 0) df.format(Date(endDate!!)) else "Vô thời hạn"
        }
        cursor.close()
    }

    /** ---------------- LABEL ---------------- */
    private fun setupLabels() {
        findViewById<View>(R.id.viewThoiHan1)
            .findViewById<TextView>(R.id.txtLabelSpinner).text = "Thời hạn hợp đồng"
        findViewById<View>(R.id.viewNgayVao1)
            .findViewById<TextView>(R.id.txtLabel).text = "Ngày bắt đầu"
        findViewById<View>(R.id.viewNgayKetThuc1)
            .findViewById<TextView>(R.id.txtLabel).text = "Ngày kết thúc"
    }

    /** ---------------- THỜI HẠN SPINNER ---------------- */
    private fun setupThoiHanSpinner() {
        val spinner = findViewById<View>(R.id.viewThoiHan1).findViewById<Spinner>(R.id.spinnerItem)
        val options = listOf("6 tháng", "1 năm", "2 năm", "Tùy chỉnh")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0) // chọn mặc định 6 tháng

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val months = when (pos) { 0 -> 6; 1 -> 12; 2 -> 24; else -> null }
                if (months != null && startDate > 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startDate
                        add(Calendar.MONTH, months)
                    }
                    endDate = cal.timeInMillis
                    val txtEnd = findViewById<View>(R.id.viewNgayKetThuc1)
                        .findViewById<TextView>(R.id.txtValue)
                    txtEnd.text = df.format(cal.time)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    /** ---------------- CHỌN NGÀY ---------------- */
    private fun setupDatePickers() {
        val viewStart = findViewById<View>(R.id.viewNgayVao1)
        val viewEnd = findViewById<View>(R.id.viewNgayKetThuc1)
        val txtStart = viewStart.findViewById<TextView>(R.id.txtValue)
        val txtEnd = viewEnd.findViewById<TextView>(R.id.txtValue)

        txtStart.setOnClickListener {
            showDatePicker { picked ->
                txtStart.text = picked
                startDate = df.parse(picked)?.time ?: 0L
                // Cập nhật ngày kết thúc tự động 6 tháng sau nếu đang chọn “6 tháng”
                val spinner = findViewById<View>(R.id.viewThoiHan1).findViewById<Spinner>(R.id.spinnerItem)
                if (spinner.selectedItemPosition == 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startDate
                        add(Calendar.MONTH, 6)
                    }
                    endDate = cal.timeInMillis
                    txtEnd.text = df.format(cal.time)
                }
            }
        }

        txtEnd.setOnClickListener {
            showDatePicker { picked ->
                txtEnd.text = picked
                endDate = df.parse(picked)?.time
                val spinner = findViewById<View>(R.id.viewThoiHan1).findViewById<Spinner>(R.id.spinnerItem)
                spinner.setSelection(3) // "Tùy chỉnh"
            }
        }
    }

    /** ---------------- NÚT LƯU ---------------- */
    private fun setupSaveButton() {
        val edtName = findViewById<EditText>(R.id.edtTenKhach)
        val edtPhone = findViewById<EditText>(R.id.edtSdtKhach)
        val edtDeposit = findViewById<EditText>(R.id.edtMucCoc)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = edtName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val deposit = edtDeposit.text.toString().toIntOrNull() ?: 0

            if (name.isEmpty() || phone.isEmpty() || startDate == 0L) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contract = Contract(
                id = contractId,
                roomId = roomId,
                tenantName = name,
                tenantPhone = phone,
                startDate = startDate,
                endDate = endDate,
                deposit = deposit,
                active = 1
            )

            if (mode == "renew") {
                val rows = dao.update(contract)
                if (rows > 0) {
                    Toast.makeText(this, "Đã gia hạn hợp đồng thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gia hạn thất bại!", Toast.LENGTH_SHORT).show()
                }
            } else {
                val id = dao.insert(contract)
                if (id > 0) {
                    val db = DatabaseHelper(this)
                    db.writableDatabase.execSQL("UPDATE rooms SET status='RENTED' WHERE id=$roomId")
                    Toast.makeText(this, "Đã thêm hợp đồng cho $roomName", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lưu hợp đồng thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** ---------------- MẶC ĐỊNH NGÀY BẮT ĐẦU + KẾT THÚC ---------------- */
    private fun setDefaultDates() {
        val viewStart = findViewById<View>(R.id.viewNgayVao1)
        val viewEnd = findViewById<View>(R.id.viewNgayKetThuc1)
        val txtStart = viewStart.findViewById<TextView>(R.id.txtValue)
        val txtEnd = viewEnd.findViewById<TextView>(R.id.txtValue)

        val now = Calendar.getInstance()
        startDate = now.timeInMillis

        val endCal = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }
        endDate = endCal.timeInMillis

        txtStart.text = df.format(now.time)
        txtEnd.text = df.format(endCal.time)
    }

    /** ---------------- DATE PICKER ---------------- */
    private fun showDatePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                onPicked(df.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
