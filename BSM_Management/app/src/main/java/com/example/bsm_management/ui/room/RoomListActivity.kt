package com.example.bsm_management.ui.room

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.R
import com.example.bsm_management.databinding.ActivityRoomListBinding
import database.DatabaseHelper

data class UiRoom(
    val id: Long,
    val name: String,
    val baseRent: Int,
    val floor: Int,
    val status: String,        // "EMPTY" | "OCCUPIED" | ...
    val tenantCount: Int,      // số người ghi nhận
    val contractEnd: String?,  // "07/09/2025 - Vô thời hạn"...
    val appUsed: Boolean,
    val onlineSigned: Boolean,
    val phone: String?         // số ĐT khách
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

        // filter icon (placeholder)
        vb.btnFilter.setOnClickListener {
            // TODO: mở bộ lọc nâng cao
        }

        // Recycler
        adapter = RoomAdapter(
            onPhoneClick = { phone ->
                if (!phone.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
            },
            onMoreClick = { roomId ->
                // TODO: hiện menu 3 chấm
            },
            onFillClick = { roomId ->
                // TODO: điều hướng lấp phòng
            },
            onPostClick = { roomId ->
                // TODO: điều hướng đăng tin
            }
        )
        vb.rvRooms.layoutManager = LinearLayoutManager(this)
        vb.rvRooms.adapter = adapter

        // FAB thêm phòng
        vb.fabAdd.setOnClickListener {
            // TODO: thêm phòng mới
        }

        loadData()
    }

    private fun loadData() {
        val db = DatabaseHelper(this)

        // Lấy toàn bộ rooms hiện có (đã tạo ở bước “Thêm nhà”)
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

                // demo dữ liệu phụ (bạn có thể thay bằng bảng thật nếu đã có)
                val tenantCount = if (status == "OCCUPIED") 1 else 0
                val appUsed = false
                val onlineSigned = false
                val contractEnd = if (status == "OCCUPIED") "07/09/2025 - Vô thời hạn" else null
                val phone: String? = if (status == "OCCUPIED") "0985857658" else null

                temp += UiRoom(
                    id, name, rent, floor, status, tenantCount,
                    contractEnd, appUsed, onlineSigned, phone
                )
            }
        }
        allRooms = temp
        floors = allRooms.map { it.floor }.distinct().sorted()
        if (floors.isEmpty()) floors = listOf(0)

        // tạo tab tầng (0 = tầng trệt)
        vb.tabs.removeAllTabs()
        floors.forEach { f ->
            vb.tabs.newTab().setText(if (f == 0) getString(R.string.floor_ground) else "Tầng $f")
                .also(vb.tabs::addTab)
        }

        vb.tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                bindFloor(floors[tab?.position ?: 0])
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // chọn tab đầu tiên
        vb.tabs.getTabAt(0)?.select()
        bindFloor(floors[0])
    }

    private fun bindFloor(floor: Int) {
        val list = allRooms.filter { it.floor == floor }
        vb.emptyView.isVisible = list.isEmpty()
        adapter.submitList(list)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
