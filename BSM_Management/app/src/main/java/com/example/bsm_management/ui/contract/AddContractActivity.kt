package com.example.bsm_management.ui.contract

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contract)

        // Edge to edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        dao = ContractDAO(this)
        roomId = intent.getIntExtra("roomId", 0)
        roomName = intent.getStringExtra("roomName") ?: "Không xác định"

        // Gọi các hàm setup
        setupHeader()
        setupDefaultsFromRoom()
        setupLabels()
        setupThoiHanSpinner()
        setupDatePickers()
        setupSaveButton()
        setupChuKySpinner()

        val mode = intent.getStringExtra("mode")
        if (mode == "renew") {
            val contractId = intent.getIntExtra("contractId", 0)
            val db = DatabaseHelper(this)
            val cursor = db.readableDatabase.rawQuery("SELECT * FROM contracts WHERE id=?", arrayOf(contractId.toString()))
            if (cursor.moveToFirst()) {
                findViewById<EditText>(R.id.edtTenKhach)
                    .setText(cursor.getString(cursor.getColumnIndexOrThrow("tenantName")))

                findViewById<EditText>(R.id.edtSdtKhach)
                    .setText(cursor.getString(cursor.getColumnIndexOrThrow("tenantPhone")))

                findViewById<EditText>(R.id.edtMucCoc)
                    .setText(cursor.getInt(cursor.getColumnIndexOrThrow("deposit")).toString())

                val start = cursor.getLong(cursor.getColumnIndexOrThrow("startDate"))
                val end = cursor.getLong(cursor.getColumnIndexOrThrow("endDate"))

                // ✅ Cách đúng: lấy include view rồi find TextView con
                val viewNgayVao = findViewById<android.view.View>(R.id.viewNgayVao1)
                val viewNgayKt = findViewById<android.view.View>(R.id.viewNgayKetThuc1)
                viewNgayVao.findViewById<TextView>(R.id.txtValue).text = df.format(Date(start))
                viewNgayKt.findViewById<TextView>(R.id.txtValue).text = df.format(Date(end))
            }
            cursor.close()
        }
    }

    // ---------------- HEADER ----------------
    private fun setupHeader() {
        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = "Lập hợp đồng mới"
        tvSubtitle.text = "Phòng $roomName"

        ivBack.setOnClickListener { finish() }
    }

    // ---------------- GÁN LABEL ----------------
    private fun setupLabels() {
        // Thời hạn hợp đồng
        val viewThoiHan = findViewById<View>(R.id.viewThoiHan1)
        viewThoiHan.findViewById<TextView>(R.id.txtLabelSpinner).text = "Thời hạn hợp đồng"

        // Ngày bắt đầu
        val viewNgayVao = findViewById<View>(R.id.viewNgayVao1)
        viewNgayVao.findViewById<TextView>(R.id.txtLabel).text = "Ngày bắt đầu"

        // Ngày kết thúc
        val viewNgayKt = findViewById<View>(R.id.viewNgayKetThuc1)
        viewNgayKt.findViewById<TextView>(R.id.txtLabel).text = "Ngày kết thúc"
    }

    /** ---------------- SPINNER THỜI HẠN ---------------- */
    private fun setupThoiHanSpinner() {
        val viewThoiHan = findViewById<View>(R.id.viewThoiHan1)
        val spinner = viewThoiHan.findViewById<Spinner>(R.id.spinnerItem)
        val lbl = viewThoiHan.findViewById<TextView>(R.id.txtLabelSpinner)

        lbl.text = "Thời hạn hợp đồng"
        val options = listOf("6 tháng", "1 năm", "2 năm", "Tùy chỉnh")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val months = when (position) {
                    0 -> 6; 1 -> 12; 2 -> 24; else -> null
                }
                // Nếu đã có startDate và chọn thời hạn cố định → tính ngày kết thúc
                if (months != null && startDate > 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startDate
                        add(Calendar.MONTH, months)
                    }
                    endDate = cal.timeInMillis
                    val viewNgayKt = findViewById<View>(R.id.viewNgayKetThuc1)
                    val txtValue = viewNgayKt.findViewById<TextView>(R.id.txtValue)
                    txtValue.text = df.format(cal.time)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** ---------------- CHỌN NGÀY BẮT ĐẦU / KẾT THÚC ---------------- */
    private fun setupDatePickers() {
        val viewNgayVao = findViewById<View>(R.id.viewNgayVao1)
        val viewNgayKetThuc = findViewById<View>(R.id.viewNgayKetThuc1)
        val txtNgayVao = viewNgayVao.findViewById<TextView>(R.id.txtValue)
        val txtNgayKt = viewNgayKetThuc.findViewById<TextView>(R.id.txtValue)

        txtNgayVao.text = "Chọn ngày"
        txtNgayKt.text = "Chọn ngày"

        txtNgayVao.setOnClickListener {
            showDatePicker { picked ->
                txtNgayVao.text = picked
                startDate = df.parse(picked)?.time ?: 0L
            }
        }

        txtNgayKt.setOnClickListener {
            showDatePicker { picked ->
                txtNgayKt.text = picked
                endDate = df.parse(picked)?.time

                // Nếu người dùng chọn thủ công → gán spinner về "Tùy chỉnh"
                val viewThoiHan = findViewById<View>(R.id.viewThoiHan1)
                val spinner = viewThoiHan.findViewById<Spinner>(R.id.spinnerItem)
                spinner.setSelection(3) // vị trí "Tùy chỉnh"
            }
        }
    }

    // ---------------- SPINNER CHU KỲ THU TIỀN ----------------
    private fun setupChuKySpinner() {
        val spnChuKy = findViewById<Spinner>(R.id.spnChuKyThuTien)
        val options = listOf("Theo tháng", "2 tháng/lần", "Theo quý", "Theo năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnChuKy.adapter = adapter
    }

    // ---------------- NÚT LƯU ----------------
    private fun setupSaveButton() {
        val edtTenantName = findViewById<EditText>(R.id.edtTenKhach)
        val edtTenantPhone = findViewById<EditText>(R.id.edtSdtKhach)
        val edtDeposit = findViewById<EditText>(R.id.edtMucCoc)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val tenantName = edtTenantName.text.toString().trim()
            val tenantPhone = edtTenantPhone.text.toString().trim()
            val deposit = edtDeposit.text.toString().toIntOrNull() ?: 0

            if (tenantName.isEmpty() || tenantPhone.isEmpty() || startDate == 0L) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contract = Contract(
                roomId = roomId,
                tenantName = tenantName,
                tenantPhone = tenantPhone,
                startDate = startDate,
                endDate = endDate,
                deposit = deposit,
                active = 1
            )

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

    // ---------------- DATE PICKER DÙNG CHUNG ----------------
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

    private fun setupDefaultsFromRoom() {
        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery(
            "SELECT baseRent FROM rooms WHERE id=?", arrayOf(roomId.toString())
        )
        if (cursor.moveToFirst()) {
            val baseRent = cursor.getInt(0)
            findViewById<EditText>(R.id.edtGiaThue).setText(baseRent.toString())
        }
        cursor.close()
    }

}
