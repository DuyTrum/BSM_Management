package com.example.bsm_management.ui.contract

import android.R.attr.mode
import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private var dobTimestamp: Long = 0


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
        findViewById<TextView>(R.id.tvDob).setOnClickListener {
            openDobPicker()
        }


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
        val options = listOf("1 năm", "2 năm", "Tùy chỉnh")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0) // chọn mặc định 6 tháng

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val months = when (pos) { 0 -> 12; 2 -> 24; else -> null }
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

        val edtTenantName = findViewById<EditText>(R.id.edtTenKhach)
        val edtTenantPhone = findViewById<EditText>(R.id.edtSdtKhach)
        val edtDeposit = findViewById<EditText>(R.id.edtMucCoc)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val edtCccd = findViewById<EditText>(R.id.edtCccd)
        val edtAddress = findViewById<EditText>(R.id.edtAddress)

        btnSave.setOnClickListener {
            val tenantName = edtTenantName.text.toString().trim()
            val tenantPhone = edtTenantPhone.text.toString().trim()
            val deposit = edtDeposit.text.toString().toIntOrNull() ?: 0

            if (tenantName.isEmpty() || tenantPhone.isEmpty() || startDate == 0L) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ====== Lưu hợp đồng ======
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

                // ====== Lưu khách thuê ======
                val cv = ContentValues().apply {
                    put("roomId", roomId)
                    put("name", tenantName)
                    put("phone", tenantPhone)
                    put("cccd", edtCccd.text.toString().trim())
                    put("address", edtAddress.text.toString().trim())
                    put("dob", dobTimestamp)
                    put("createdAt", System.currentTimeMillis())
                    put("slotIndex", 1)
                    put("isOld", 0)
                }
                db.writableDatabase.insert("tenants", null, cv)

                db.writableDatabase.execSQL("UPDATE rooms SET status='RENTED' WHERE id=$roomId")

                Toast.makeText(this, "Đã thêm hợp đồng & khách thuê", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Lưu hợp đồng thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    /** ---------------- MẶC ĐỊNH NGÀY BẮT ĐẦU + KẾT THÚC ---------------- */
    private fun setDefaultDates() {
        val viewStart = findViewById<View>(R.id.viewNgayVao1)
        val viewEnd = findViewById<View>(R.id.viewNgayKetThuc1)
        val txtStart = viewStart.findViewById<TextView>(R.id.txtValue)
        val txtEnd = viewEnd.findViewById<TextView>(R.id.txtValue)

        // Lấy ngày hôm nay theo format dd/MM/yyyy → parse về timestamp 00:00
        val todayString = df.format(Date())
        val today = df.parse(todayString)!!.time

        startDate = today

        // Tính ngày kết thúc 6 tháng sau
        val cal = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.MONTH, 6)
        }
        endDate = cal.timeInMillis

        // Hiển thị ra UI
        txtStart.text = df.format(Date(startDate))
        txtEnd.text = df.format(Date(endDate!!))
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
    private fun openDobPicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_dob_picker, null)
        dialog.setContentView(view)

        val npDay = view.findViewById<NumberPicker>(R.id.npDay)
        val npMonth = view.findViewById<NumberPicker>(R.id.npMonth)
        val npYear = view.findViewById<NumberPicker>(R.id.npYear)
        val btnDone = view.findViewById<Button>(R.id.btnDoneDob)

        npDay.minValue = 1
        npDay.maxValue = 31

        npMonth.minValue = 1
        npMonth.maxValue = 12

        npYear.minValue = 1950
        npYear.maxValue = 2025
        npYear.value = 2000 // default

        btnDone.setOnClickListener {
            val d = npDay.value
            val m = npMonth.value
            val y = npYear.value

            val cal = Calendar.getInstance()
            cal.set(y, m - 1, d)

            // set text lên UI
            findViewById<TextView>(R.id.tvDob).text =
                "%02d/%02d/%04d".format(d, m, y)

            // lưu timestamp tạm vào biến
            dobTimestamp = cal.timeInMillis

            dialog.dismiss()
        }

        dialog.show()
    }

}
