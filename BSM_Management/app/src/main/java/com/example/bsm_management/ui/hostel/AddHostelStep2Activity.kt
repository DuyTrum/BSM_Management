package com.example.bsm_management.ui.hostel

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

    private var sampleRooms = 0
    private var price = 0
    private var maxPeople = 0

    // Dịch vụ được bật/tắt
    private val serviceStates = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hostel_step2)

        // Nhận dữ liệu từ Bước 1
        sampleRooms = intent.getIntExtra("sampleRooms", 0)
        price = intent.getIntExtra("price", 0)
        maxPeople = intent.getIntExtra("maxPeople", 0)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { finish() }

        // ==== Dịch vụ ====
        setupService(R.id.svcElectric, "Dịch vụ điện", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcWater, "Dịch vụ nước", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcTrash, "Dịch vụ rác", "Miễn phí / không sử dụng")
        setupService(R.id.svcInternet, "Dịch vụ internet/mạng", "Miễn phí / không sử dụng")

        // Lưu nhà trọ
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            if (sampleRooms <= 0) {
                Toast.makeText(this, "Số phòng không hợp lệ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveHostel()
        }
    }

    /** ============================
     *      LƯU DỮ LIỆU
     *  ============================ */
    private fun saveHostel() {

        val db = DatabaseHelper(this).writableDatabase
        db.beginTransaction()

        try {
            // Xoá sạch dữ liệu cũ (không xóa dịch vụ của app khác)
            db.execSQL("DELETE FROM rooms")
            db.execSQL("DELETE FROM services")

            // Statement tạo phòng
            val insertRoom = db.compileStatement("""
            INSERT INTO rooms (name, floor, status, baseRent, maxPeople)
            VALUES (?, 1, 'EMPTY', ?, ?)
        """)

            // Statement tạo dịch vụ theo phòng
            val insertSvc = db.compileStatement("""
            INSERT INTO services (roomId, serviceName, enabled, price)
            VALUES (?, ?, ?, 0)
        """)

            val allServices = listOf("Dịch vụ điện", "Dịch vụ nước", "Dịch vụ rác", "Dịch vụ internet/mạng")

            for (i in 1..sampleRooms) {

                val roomName = "P%03d".format(i)

                insertRoom.bindString(1, roomName)
                insertRoom.bindLong(2, price.toLong())
                insertRoom.bindLong(3, maxPeople.toLong())

                val roomId = insertRoom.executeInsert()  // LẤY ID PHÒNG

                // tạo dịch vụ theo trạng thái user chọn
                allServices.forEach { svc ->
                    insertSvc.bindLong(1, roomId)
                    insertSvc.bindString(2, svc)
                    insertSvc.bindLong(3, if (serviceStates[svc] == true) 1 else 0)
                    insertSvc.executeInsert()
                }
            }

            db.setTransactionSuccessful()

        } finally {
            db.endTransaction()
        }

        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }


    /** Cài đặt checkbox dịch vụ */
    private fun setupService(rootId: Int, title: String, desc: String) {

        val root = findViewById<View>(rootId)
        val tvTitle = root.findViewById<TextView>(R.id.tvServiceTitle)
        val tvDesc = root.findViewById<TextView>(R.id.tvServiceDesc)
        val sw = root.findViewById<MaterialSwitch>(R.id.swService)

        tvTitle.text = title
        tvDesc.text = desc

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
