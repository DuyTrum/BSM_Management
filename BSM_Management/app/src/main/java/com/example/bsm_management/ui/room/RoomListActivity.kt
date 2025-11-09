package com.example.bsm_management.ui.room

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.R
import com.example.bsm_management.databinding.ActivityRoomListBinding
import database.DatabaseHelper

class RoomListActivity : AppCompatActivity() {

    private lateinit var vb: ActivityRoomListBinding
    private lateinit var adapter: RoomAdapter
    private var allRooms: List<UiRoom> = emptyList()
    private var floors: List<Int> = listOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityRoomListBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setSupportActionBar(vb.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Danh sách phòng"

        val db = DatabaseHelper(this)
        adapter = RoomAdapter(
            onPhoneClick = { phone ->
                if (!phone.isNullOrBlank()) startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                else Toast.makeText(this, "Phòng này chưa có số điện thoại", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { roomId -> showRoomOptions(roomId) },
            onFillClick = { roomId ->
                val room = allRooms.find { it.id == roomId }
                updateRoomStatus(roomId, room?.status)
            },
            onPostClick = { roomId ->
                Toast.makeText(this, "Đăng tin cho phòng #$roomId (đang phát triển)", Toast.LENGTH_SHORT).show()
            },
            onEditRoomPriceClick = { id, price -> editRoomPrice(id, price) }
        )

        vb.rvRooms.layoutManager = LinearLayoutManager(this)
        vb.rvRooms.adapter = adapter

        adapter.setActiveServices(db.getAllServicesDetailed())
        vb.fabAdd.setOnClickListener { addNewRoom() }
        loadData()
    }

    private fun loadData() {
        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery(
            "SELECT id, name, floor, baseRent, status FROM rooms ORDER BY floor, name", null
        )
        val temp = mutableListOf<UiRoom>()
        cursor.use {
            while (it.moveToNext()) {
                temp += UiRoom(
                    id = it.getLong(0),
                    name = it.getString(1),
                    baseRent = it.getInt(3),
                    floor = it.getInt(2),
                    status = it.getString(4),
                    tenantCount = null,
                    contractEnd = null,
                    appUsed = null,
                    onlineSigned = null,
                    phone = null
                )
            }
        }
        allRooms = temp
        vb.emptyView.isVisible = allRooms.isEmpty()
        adapter.submitList(allRooms)
    }

    private fun updateRoomStatus(roomId: Long, currentStatus: String?) {
        val newStatus = if (currentStatus == "OCCUPIED") "EMPTY" else "OCCUPIED"
        val db = DatabaseHelper(this).writableDatabase
        val cv = ContentValues().apply { put("status", newStatus) }
        db.update("rooms", cv, "id=?", arrayOf(roomId.toString()))
        loadData()
    }

    private fun editRoomPrice(roomId: Long, currentPrice: Int?) {
        val input = EditText(this).apply {
            hint = "Nhập giá mới (đ/tháng)"
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

    private fun addNewRoom() {
        val db = DatabaseHelper(this).writableDatabase
        val cv = ContentValues().apply {
            put("name", "P${System.currentTimeMillis() % 1000}")
            put("status", "EMPTY")
            put("baseRent", 0)
        }
        db.insert("rooms", null, cv)
        loadData()
    }

    private fun showRoomOptions(roomId: Long) {
        val items = arrayOf("Đổi trạng thái", "Xóa phòng")
        AlertDialog.Builder(this)
            .setTitle("Phòng #$roomId")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> updateRoomStatus(roomId, allRooms.find { it.id == roomId }?.status)
                    1 -> {
                        val db = DatabaseHelper(this).writableDatabase
                        db.delete("rooms", "id=?", arrayOf(roomId.toString()))
                        loadData()
                    }
                }
            }.show()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
