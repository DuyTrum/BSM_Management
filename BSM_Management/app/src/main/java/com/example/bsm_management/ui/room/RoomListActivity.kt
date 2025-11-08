package com.example.bsm_management.ui.room

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.R
import com.example.bsm_management.databinding.ActivityRoomListBinding
import database.DatabaseHelper

data class UiRoom(
    val id: Long,
    val name: String,
    val baseRent: Int?,
    val floor: Int?,
    val status: String?,
    val tenantCount: Int?,
    val contractEnd: String?,
    val appUsed: Boolean?,
    val onlineSigned: Boolean?,
    val phone: String?
)

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
        supportActionBar?.title = getString(R.string.title_room_list)

        adapter = RoomAdapter(
            onPhoneClick = { phone ->
                if (!phone.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                } else {
                    Toast.makeText(this, "Phòng này chưa có số điện thoại", Toast.LENGTH_SHORT).show()
                }
            },
            onMoreClick = { roomId ->
                showRoomOptions(roomId)
            },
            onFillClick = { roomId ->
                val room = allRooms.find { it.id == roomId }
                updateRoomStatus(roomId, room?.status)
            },
            onPostClick = { roomId ->
                Toast.makeText(this, "Đăng tin cho phòng #$roomId (đang phát triển)", Toast.LENGTH_SHORT).show()
            }
        )

        vb.rvRooms.layoutManager = LinearLayoutManager(this)
        vb.rvRooms.adapter = adapter

        vb.fabAdd.setOnClickListener { addNewRoom() }

        loadData()
        testContentProvider()
        insertRoomViaProvider()
    }

    // ================== ĐỌC DỮ LIỆU PHÒNG ==================
    private fun loadData() {
        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery(
            "SELECT id, name, floor, baseRent, status FROM rooms ORDER BY floor, name", null
        )

        val temp = mutableListOf<UiRoom>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1)
                val floor = it.getInt(2)
                val rent = it.getInt(3)
                val status = it.getString(4)
                temp += UiRoom(
                    id = id,
                    name = name,
                    baseRent = rent,
                    floor = floor,
                    status = status,
                    tenantCount = null,
                    contractEnd = null,
                    appUsed = null,
                    onlineSigned = null,
                    phone = null
                )
            }
        }

        allRooms = temp
        floors = allRooms.mapNotNull { it.floor }.distinct().sorted()
        if (floors.isEmpty()) floors = listOf(0)

        vb.tabs.removeAllTabs()
        floors.forEach { f ->
            vb.tabs.newTab()
                .setText(if (f == 0) getString(R.string.floor_ground) else "Tầng $f")
                .also(vb.tabs::addTab)
        }

        vb.tabs.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                bindFloor(floors[tab?.position ?: 0])
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        vb.tabs.getTabAt(0)?.select()
        bindFloor(floors[0])
    }

    private fun bindFloor(floor: Int) {
        val list = allRooms.filter { it.floor == floor }
        vb.emptyView.isVisible = list.isEmpty()
        adapter.submitList(list)
    }

    // ================== CẬP NHẬT TRẠNG THÁI ==================
    private fun updateRoomStatus(roomId: Long, currentStatus: String?) {
        val newStatus = if (currentStatus == "OCCUPIED") "EMPTY" else "OCCUPIED"
        val db = DatabaseHelper(this).writableDatabase
        val cv = ContentValues().apply { put("status", newStatus) }
        val rows = db.update("rooms", cv, "id = ?", arrayOf(roomId.toString()))
        if (rows > 0) {
            val msg = if (newStatus == "OCCUPIED") "Phòng đã được lấp" else "Phòng đã được trả"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            loadData()
        } else Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
    }

    // ================== XÓA PHÒNG ==================
    private fun deleteRoom(roomId: Long) {
        val db = DatabaseHelper(this).writableDatabase
        val rows = db.delete("rooms", "id = ?", arrayOf(roomId.toString()))
        if (rows > 0) {
            Toast.makeText(this, "Đã xóa phòng #$roomId", Toast.LENGTH_SHORT).show()
            loadData()
        } else Toast.makeText(this, "Không thể xóa phòng", Toast.LENGTH_SHORT).show()
    }

    // ================== THÊM PHÒNG MỚI ==================
    private fun addNewRoom() {
        val db = DatabaseHelper(this).writableDatabase
        val cv = ContentValues().apply {
            put("name", "P${System.currentTimeMillis() % 1000}")
            put("floor", 1)
            put("status", "EMPTY")
            put("baseRent", 10000)
        }
        db.insert("rooms", null, cv)
        Toast.makeText(this, "Đã thêm phòng mới", Toast.LENGTH_SHORT).show()
        loadData()
    }

    // ================== MENU 3 CHẤM ==================
    private fun showRoomOptions(roomId: Long) {
        val items = arrayOf("Đổi trạng thái", "Xóa phòng", "Chi tiết")
        AlertDialog.Builder(this)
            .setTitle("Tùy chọn phòng #$roomId")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val room = allRooms.find { it.id == roomId }
                        updateRoomStatus(roomId, room?.status)
                    }
                    1 -> deleteRoom(roomId)
                    2 -> Toast.makeText(this, "Chi tiết phòng đang phát triển", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }
    // ================== TEST ContentProvider ==================
    private fun testContentProvider() {
        val uri = Uri.parse("content://com.example.bsm_management.ui.provider/rooms")
        val cursor = contentResolver.query(uri, arrayOf("id", "name", "status"), null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val sb = StringBuilder()
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                sb.append("Phòng #$id - $name - $status\n")
            } while (cursor.moveToNext())
            cursor.close()

            Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Không có dữ liệu từ ContentProvider", Toast.LENGTH_SHORT).show()
        }
    }

    // ================== TEST INSERT QUA PROVIDER ==================
    private fun insertRoomViaProvider() {
        val uri = Uri.parse("content://com.example.bsm_management.ui.provider/rooms")
        val values = ContentValues().apply {
            put("name", "P_Test")
            put("floor", 2)
            put("status", "EMPTY")
            put("baseRent", 1500000)
        }
        val newUri = contentResolver.insert(uri, values)
        Toast.makeText(this, "Đã thêm phòng qua Provider: $newUri", Toast.LENGTH_SHORT).show()
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }

}
