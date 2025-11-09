package com.example.bsm_management.ui.hostel

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.view.View
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

    // Danh sách dịch vụ lưu tạm để ghi DB
    private val serviceStates = mutableMapOf<String, Boolean>()

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
        setupService(R.id.svcTrash, "Dịch vụ rác", "Miễn phí / không sử dụng")
        setupService(R.id.svcInternet, "Dịch vụ internet/mạng", "Miễn phí / không sử dụng")

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
                // 🏠 Tạo phòng mẫu
                for (i in 1..sampleRooms) {
                    val cv = ContentValues().apply {
                        put("name", "P%03d".format(i))
                        put("floor", 1)
                        put("status", "EMPTY")
                        put("baseRent", price)
                    }
                    dbw.insertOrThrow("rooms", null, cv)
                }

                // 💾 Lưu danh sách dịch vụ
                dbw.execSQL("CREATE TABLE IF NOT EXISTS services (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "enabled INTEGER NOT NULL DEFAULT 0)")
                dbw.execSQL("DELETE FROM services")

                val insertSvc = dbw.compileStatement(
                    "INSERT INTO services (name, enabled) VALUES (?, ?)"
                )
                serviceStates.forEach { (name, enabled) ->
                    insertSvc.bindString(1, name)
                    insertSvc.bindLong(2, if (enabled) 1 else 0)
                    insertSvc.executeInsert()
                }

                dbw.setTransactionSuccessful()
                Toast.makeText(this, "Đã tạo $sampleRooms phòng và lưu dịch vụ.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi lưu: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                dbw.endTransaction()
            }

            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }

    /** Cấu hình mỗi dòng dịch vụ */
    private fun setupService(rootId: Int, title: String, desc: String) {
        val root = findViewById<View>(rootId)
        val tvTitle = root.findViewById<TextView>(R.id.tvServiceTitle)
        val tvDesc = root.findViewById<TextView>(R.id.tvServiceDesc)
        val sw = root.findViewById<MaterialSwitch>(R.id.swService)

        tvTitle.text = title
        tvDesc.text = desc

        // Giá trị mặc định (điện & nước bật, rác & internet tắt)
        val defaultChecked = title.contains("điện") || title.contains("nước")
        sw.isChecked = defaultChecked
        serviceStates[title] = defaultChecked

        sw.setOnCheckedChangeListener { _, isChecked ->
            serviceStates[title] = isChecked
            tvDesc.text = if (isChecked) "Đang sử dụng" else "Miễn phí / không sử dụng"
        }
    }

    private fun setupFeature(rootId: Int, icon: Int, title: String, desc: String) {
        val root = findViewById<View>(rootId)
        root.findViewById<ImageView>(R.id.imgFeatureIcon).setImageResource(icon)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
        root.findViewById<MaterialSwitch>(R.id.swFeature).isChecked = true
    }
}
