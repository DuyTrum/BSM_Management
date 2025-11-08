package com.example.bsm_management.ui.contract

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.dao.ContractDAO
import java.text.SimpleDateFormat
import java.util.*

class ContractDetailActivity : AppCompatActivity() {

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var dao: ContractDAO
    private var contractId: Int = -1
    private var tenantPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contract_detail)

        // Xử lý padding full màn hình
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        dao = ContractDAO(this)
        contractId = intent.getIntExtra("contractId", -1)

        if (contractId == -1) {
            Toast.makeText(this, "Không tìm thấy hợp đồng!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val contract = dao.getById(contractId)
        if (contract == null) {
            Toast.makeText(this, "Hợp đồng không tồn tại!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tenantPhone = contract.tenantPhone

        // 🔹 Set tiêu đề header
        setupHeader(contract.roomId)

        // 🔹 Hiển thị thông tin
        findViewById<TextView>(R.id.tvRoomName).text = "Phòng ${contract.roomId}"
        findViewById<TextView>(R.id.tvTenant).text = "Khách thuê: ${contract.tenantName}"

        val startDate = df.format(Date(contract.startDate))
        val endDate = if (contract.endDate != null) df.format(Date(contract.endDate!!)) else "Vô thời hạn"

        findViewById<TextView>(R.id.tvDuration).text = "$startDate - $endDate"
        findViewById<TextView>(R.id.tvRent).text = "Theo giá phòng trong CSDL"
        findViewById<TextView>(R.id.tvDeposit).text = "%,d ₫".format(contract.deposit)
        findViewById<TextView>(R.id.tvStatus).text =
            if (contract.active == 1) "Đang hiệu lực" else "Đã hết hạn"
        findViewById<TextView>(R.id.tvPhone).text = "📞 ${contract.tenantPhone}"
        findViewById<TextView>(R.id.tvNote).text =
            "Hợp đồng ${if (contract.endDate != null) "có thời hạn" else "vô thời hạn"}, cọc ${"%,d".format(contract.deposit)} ₫."

        setupActions()
    }

    /** ---------------- HEADER ---------------- */
    private fun setupHeader(roomId: Int) {
        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = "Chi tiết hợp đồng"
        tvSubtitle.text = "Phòng $roomId"

        ivBack.setOnClickListener { finish() }
    }

    /** ---------------- ACTIONS ---------------- */
    private fun setupActions() {
        val btnCall = findViewById<LinearLayout>(R.id.btnCall)
        val btnShare = findViewById<LinearLayout>(R.id.btnShare)
        val btnPrint = findViewById<LinearLayout>(R.id.btnPrint)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        btnCall.setOnClickListener {
            if (tenantPhone.isBlank()) {
                Toast.makeText(this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tenantPhone"))
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener {
            val content = """
                🏠 ${findViewById<TextView>(R.id.tvRoomName).text}
                👤 ${findViewById<TextView>(R.id.tvTenant).text}
                ⏱ ${findViewById<TextView>(R.id.tvDuration).text}
                💰 ${findViewById<TextView>(R.id.tvDeposit).text}
                📋 ${findViewById<TextView>(R.id.tvStatus).text}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Thông tin hợp đồng")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
        }

        btnPrint.setOnClickListener {
            Toast.makeText(this, "Tính năng In hợp đồng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa hợp đồng")
                .setMessage("Bạn có chắc muốn xóa hợp đồng này?")
                .setPositiveButton("Xóa") { _, _ ->
                    val result = dao.delete(contractId)
                    if (result > 0) {
                        Toast.makeText(this, "Đã xóa hợp đồng!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Không thể xóa!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
}
