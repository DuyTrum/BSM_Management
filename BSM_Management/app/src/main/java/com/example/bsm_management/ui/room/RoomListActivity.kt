package com.example.bsm_management.ui.room

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.ActivityRoomListBinding
import com.example.bsm_management.ui.tenant.TenantListActivity
import database.DatabaseHelper

class RoomListActivity : AppCompatActivity() {

    private lateinit var vb: ActivityRoomListBinding
    private lateinit var adapter: RoomAdapter
    private var allRooms: List<UiRoom> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityRoomListBinding.inflate(layoutInflater)
        setContentView(vb.root)

        ViewCompat.setOnApplyWindowInsetsListener(vb.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // HEADER
        setSupportActionBar(vb.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Danh sách phòng"

        adapter = RoomAdapter(

            // NHẤN VÀO PHÒNG → XEM KHÁCH THUÊ
            onRoomClick = { room ->
                if (room.status == "EMPTY") {
                    Toast.makeText(this, "Phòng chưa được thuê!", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, TenantListActivity::class.java)
                    intent.putExtra("roomId", room.id.toInt())
                    intent.putExtra("roomName", room.name)
                    intent.putExtra("maxPeople", room.maxPeople ?: 1)
                    startActivity(intent)
                }
            },

            // GỌI ĐIỆN CHO KHÁCH TRONG PHÒNG
            onPhoneClick = { phone ->
                if (!phone.isNullOrBlank())
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                else Toast.makeText(this, "Phòng này chưa có số điện thoại", Toast.LENGTH_SHORT).show()
            },

            // MENU SỬA/XÓA
            onMoreClick = { roomId ->
                showRoomOptions(roomId)
            },

            // LẤP PHÒNG → TRẢ PHÒNG
            onFillClick = { roomId ->
                val r = allRooms.find { it.id == roomId }
                if (r?.status == "EMPTY") {
                    val intent = Intent(this, com.example.bsm_management.ui.contract.AddContractActivity::class.java)
                    intent.putExtra("roomId", roomId.toInt())
                    intent.putExtra("roomName", r?.name)
                    startActivity(intent)
                } else {
                    val db = DatabaseHelper(this).readableDatabase
                    db.rawQuery(
                        "SELECT id FROM contracts WHERE roomId = ? AND active = 1 ORDER BY id DESC LIMIT 1",
                        arrayOf(roomId.toString())
                    ).use { c ->
                        if (c.moveToFirst()) {
                            val contractId = c.getInt(0)
                            val intent = Intent(this, com.example.bsm_management.ui.contract.ContractDetailActivity::class.java)
                            intent.putExtra("contractId", contractId)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Không tìm thấy hợp đồng đang hiệu lực cho phòng này", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },

            // SỬA DỊCH VỤ THEO TỪNG PHÒNG
            onEditServicePriceClick = { roomId, serviceName, currentPrice ->
                editServiceForRoom(roomId, serviceName, currentPrice)
            },

            // ĐỔI GIÁ THUÊ PHÒNG
            onEditRoomPriceClick = { roomId, price ->
                editRoomPrice(roomId, price)
            }
        )

        vb.rvRooms.layoutManager = LinearLayoutManager(this)
        vb.rvRooms.adapter = adapter

        vb.fabAdd.setOnClickListener {
            addNewRoom()
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // ============================================================
    // LOAD ROOMS
    // ============================================================
    private fun loadData() {
        val db = DatabaseHelper(this)

        val cursor = db.readableDatabase.rawQuery(
            "SELECT id, name, floor, baseRent, status, maxPeople, tenantPhone FROM rooms ORDER BY floor, name", null
        )

        val list = mutableListOf<UiRoom>()

        cursor.use {
            while (it.moveToNext()) {
                val roomId = it.getLong(0)

                list += UiRoom(
                    id = roomId,
                    name = it.getString(1),
                    baseRent = it.getInt(3),
                    floor = it.getInt(2),
                    status = it.getString(4),
                    maxPeople = it.getInt(5),
                    phone = it.getString(6),
                    tenantCount = null,
                    contractEnd = null,
                    appUsed = null,
                    onlineSigned = null,

                    // ⚡ Load dịch vụ theo từng phòng
                    services = db.getServicesForRoom(roomId)
                )
            }
        }

        allRooms = list
        vb.emptyView.isVisible = allRooms.isEmpty()

        adapter.submitList(allRooms)
    }

    // ============================================================
    // EDIT SERVICE (ROOM-SPECIFIC)
    // ============================================================
    private fun editServiceForRoom(roomId: Long, serviceName: String, currentPrice: Int?) {

        val items = arrayOf("Bật dịch vụ", "Tắt dịch vụ", "Đổi giá")

        AlertDialog.Builder(this)
            .setTitle(serviceName)
            .setItems(items) { _, which ->
                val db = DatabaseHelper(this)

                when (which) {

                    0 -> {
                        db.updateRoomService(roomId, serviceName, enabled = true)
                        loadData()
                    }

                    1 -> {
                        db.updateRoomService(roomId, serviceName, enabled = false)
                        loadData()
                    }

                    2 -> {
                        editRoomServicePrice(roomId, serviceName, currentPrice)
                    }
                }
            }
            .show()
    }

    private fun editRoomServicePrice(roomId: Long, serviceName: String, currentPrice: Int?) {

        val input = EditText(this).apply {
            hint = "Nhập giá mới"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentPrice?.toString() ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("Giá $serviceName")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->

                val newPrice = input.text.toString().toIntOrNull()
                if (newPrice != null && newPrice >= 0) {
                    val db = DatabaseHelper(this)
                    db.updateRoomService(roomId, serviceName, enabled = true, price = newPrice)
                    loadData()
                }

            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ============================================================
    // EDIT ROOM PRICE
    // ============================================================
    private fun editRoomPrice(roomId: Long, currentPrice: Int?) {

        val input = EditText(this).apply {
            hint = "Nhập giá thuê mới"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentPrice?.toString() ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("Chỉnh giá thuê phòng")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newPrice = input.text.toString().toIntOrNull()
                if (newPrice != null && newPrice > 0) {
                    val db = DatabaseHelper(this).writableDatabase
                    val cv = ContentValues().apply { put("baseRent", newPrice) }
                    db.update("rooms", cv, "id=?", arrayOf(roomId.toString()))
                    loadData()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ============================================================
    // ADD NEW ROOM
    // ============================================================
    private fun addNewRoom() {

        val dbh = DatabaseHelper(this)
        val db = dbh.writableDatabase

        // ===== Lấy room index tiếp theo =====
        val cursor = db.rawQuery("SELECT MAX(id) FROM rooms", null)
        var nextIndex = 1
        cursor.use {
            if (it.moveToFirst()) {
                val maxId = it.getInt(0)
                nextIndex = maxId + 1
            }
        }
        val roomName = "P%03d".format(nextIndex)

        // ===== Lấy giá & maxPeople từ phòng có sẵn =====
        val configCursor = db.rawQuery(
            "SELECT baseRent, maxPeople FROM rooms LIMIT 1", null
        )
        var defaultRent = 0
        var defaultMax = 0

        configCursor.use {
            if (it.moveToFirst()) {
                defaultRent = it.getInt(0)
                defaultMax = it.getInt(1)
            }
        }

        // ===== Tạo phòng mới =====
        val cv = ContentValues().apply {
            put("name", roomName)
            put("status", "EMPTY")
            put("baseRent", defaultRent)
            put("maxPeople", defaultMax)
        }

        val id = db.insert("rooms", null, cv)

        // ===== Tạo dịch vụ mặc định cho phòng =====
        dbh.createDefaultServicesForRoom(id)

        loadData()
    }


    // ============================================================
    // EDIT PHONE
    // ============================================================
    private fun editRoomPhone(roomId: Long) {
        val input = EditText(this).apply {
            hint = "Nhập số điện thoại"
            inputType = InputType.TYPE_CLASS_PHONE
            val r = allRooms.find { it.id == roomId }
            setText(r?.phone ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("Sửa số điện thoại phòng")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newPhone = input.text.toString().trim()
                val db = DatabaseHelper(this).writableDatabase
                val cv = ContentValues().apply { put("tenantPhone", newPhone) }
                db.update("rooms", cv, "id=?", arrayOf(roomId.toString()))
                loadData()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ============================================================
    // OPTIONS MENU
    // ============================================================
    private fun showRoomOptions(roomId: Long) {

        val items = arrayOf("Xóa phòng", "Chỉnh giá thuê", "Sửa số điện thoại","Chỉnh số người tối đa")

        AlertDialog.Builder(this)
            .setTitle("Phòng #$roomId")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        DatabaseHelper(this).writableDatabase
                            .delete("rooms", "id=?", arrayOf(roomId.toString()))
                        loadData()
                    }
                    1 -> editRoomPrice(roomId, allRooms.find { it.id == roomId }?.baseRent)
                    2 -> editRoomPhone(roomId)
                    3 -> editMaxPeople(roomId, allRooms.find { it.id == roomId }?.maxPeople ?: 1)
                }
            }
            .show()
    }
    private fun editMaxPeople(roomId: Long, currentMax: Int) {

        val input = EditText(this).apply {
            hint = "Nhập số người tối đa"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentMax.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Chỉnh số người tối đa")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newValue = input.text.toString().toIntOrNull()

                if (newValue != null && newValue >= 0) {

                    val db = DatabaseHelper(this).writableDatabase
                    val cv = ContentValues().apply { put("maxPeople", newValue) }

                    db.update("rooms", cv, "id=?", arrayOf(roomId.toString()))

                    Toast.makeText(this, "Đã cập nhật số người tối đa", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this, "Giá trị không hợp lệ!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
