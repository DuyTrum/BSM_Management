package com.example.bsm_management.ui.hostel

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import database.DatabaseHelper

class AddHostelStep2Activity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var sampleRooms = 0
    private var price = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hostel_step2)
        db = DatabaseHelper(this)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { finish() }

        // nhận dữ liệu
        val name = intent.getStringExtra("name") ?: ""
        sampleRooms = intent.getIntExtra("sampleRooms", 0)
        price = intent.getIntExtra("price", 0)

        // ==== Dịch vụ ====
        setupService(R.id.svcElectric, "Dịch vụ điện", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcWater, "Dịch vụ nước", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcTrash, "Dịch vụ rác", "Miễn phí tiền điện/không sử dụng")
        setupService(R.id.svcInternet, "Dịch vụ internet/mạng", "Miễn phí tiền điện/không sử dụng")

        // ==== Tính năng ====
        setupFeature(
            R.id.featApp,
            R.drawable.ic_app,
            "APP dành riêng cho khách thuê",
            "Tạo & kết nối dễ dàng, hoá đơn tự động, ký hợp đồng online…"
        )
        setupFeature(
            R.id.featZalo,
            R.drawable.ic_zalo,
            "Gửi hoá đơn tự động qua ZALO",
            "Dễ dàng gửi hoá đơn hàng loạt qua ZALO"
        )
        setupFeature(
            R.id.featImage,
            R.drawable.ic_file,
            "Hình ảnh, File chứng từ hợp đồng",
            "Hình ảnh CCCD, hợp đồng giấy,…"
        )

        // === Lưu ===
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            if (sampleRooms <= 0) {
                Toast.makeText(this, "Số phòng không hợp lệ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbw = db.writableDatabase
            dbw.beginTransaction()
            try {
                for (i in 1..sampleRooms) {
                    val cv = ContentValues().apply {
                        put("name", "P%03d".format(i))
                        put("floor", 1)
                        put("status", "EMPTY")
                        put("baseRent", price)
                    }
                    dbw.insertOrThrow("rooms", null, cv)
                }
                dbw.setTransactionSuccessful()
                Toast.makeText(this, "Đã tạo $sampleRooms phòng trống.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi lưu: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                dbw.endTransaction()
            }

            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    private fun setupService(rootId: Int, title: String, desc: String) {
        val root = findViewById<android.view.View>(rootId)
        val tvTitle = root.findViewById<TextView>(R.id.tvServiceTitle)
        val tvDesc = root.findViewById<TextView>(R.id.tvServiceDesc)
        val btnClear = root.findViewById<ImageButton>(R.id.btnClear)

        tvTitle.text = title
        tvDesc.text = desc
        btnClear.setOnClickListener {
            tvDesc.text = "Miễn phí / không sử dụng"
            Toast.makeText(this, "$title: đã đặt miễn phí", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFeature(rootId: Int, icon: Int, title: String, desc: String) {
        val root = findViewById<android.view.View>(rootId)
        root.findViewById<ImageView>(R.id.imgFeatureIcon).setImageResource(icon)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
        root.findViewById<MaterialSwitch>(R.id.swFeature).isChecked = true
    }
}
