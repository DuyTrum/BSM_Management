package com.example.bsm_management.ui.contract

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddContractActivity : AppCompatActivity() {

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_contract)

        // root trong activity_add_contract.xml phải có android:id="@+id/main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<View>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }


        setupSectionHeaders()
        setupThoiHanSpinner()
        setupNgayBatDau_KetThuc()
        setupChuKyThuTienSpinner()
        setupBackButtonIfAny()
    }

    private fun setupSectionHeaders() {
        findHeader(R.id.headerThoiHan1)?.apply {
            findViewById<TextView>(R.id.tvTitle)?.text = "Thông tin thời hạn hợp đồng"
            findViewById<TextView>(R.id.tvSubtitle)?.text = "Thiết lập thời hạn cho hợp đồng mới"
        }
        findHeader(R.id.headerThanhVien)?.apply {
            findViewById<TextView>(R.id.tvTitle)?.text = "Thông tin khách thuê"
            findViewById<TextView>(R.id.tvSubtitle)?.text = "Nhập thông tin người thuê & số thành viên"
        }
        findHeader(R.id.headerGiaTri)?.apply {
            findViewById<TextView>(R.id.tvTitle)?.text = "Thông tin hợp đồng"
            findViewById<TextView>(R.id.tvSubtitle)?.text = "Giá thuê, mức cọc, chu kỳ & ngày thu"
        }
    }

    private fun setupThoiHanSpinner() {
        val thoiHanView = findViewById<View>(R.id.viewThoiHan1)
        // ⚠️ id bên trong include phải khớp với layout thật
        val lbl = thoiHanView?.findViewById<TextView>(R.id.txtLabelSpinner)
        val spn = thoiHanView?.findViewById<Spinner>(R.id.spinnerItem)

        lbl?.text = "Thời hạn"
        val options = listOf("6 tháng", "12 tháng", "24 tháng", "Không thời hạn")
        spn?.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spn?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Toast.makeText(this@AddContractActivity, "Đã chọn: ${options[position]}", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupNgayBatDau_KetThuc() {
        val ngayVaoView = findViewById<View>(R.id.viewNgayVao1)
        val lblNgayVao = ngayVaoView?.findViewById<TextView>(R.id.txtLabel)
        val valNgayVao = ngayVaoView?.findViewById<TextView>(R.id.txtValue)
        lblNgayVao?.text = "Ngày bắt đầu"
        valNgayVao?.apply {
            text = "Chọn ngày"
            setOnClickListener { showDatePicker { picked -> text = picked } }
        }

        val ngayKtView = findViewById<View>(R.id.viewNgayKetThuc1)
        val lblNgayKt = ngayKtView?.findViewById<TextView>(R.id.txtLabel)
        val valNgayKt = ngayKtView?.findViewById<TextView>(R.id.txtValue)
        lblNgayKt?.text = "Ngày kết thúc"
        valNgayKt?.apply {
            text = "Chọn ngày"
            setOnClickListener { showDatePicker { picked -> text = picked } }
        }
    }

    private fun setupChuKyThuTienSpinner() {
        val spnChuKy = findViewById<Spinner>(R.id.spnChuKyThuTien)
        val options = listOf("Theo tháng", "2 tháng/lần", "Theo quý", "Theo năm")
        spnChuKy?.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun showDatePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                onPicked(df.format(cal.time))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun findHeader(headerId: Int): View? = findViewById(headerId)

    private fun setupBackButtonIfAny() {
        val backBtnId = resources.getIdentifier("btnBack", "id", packageName)
        if (backBtnId != 0) findViewById<View>(backBtnId)?.setOnClickListener { finish() }
    }
}
