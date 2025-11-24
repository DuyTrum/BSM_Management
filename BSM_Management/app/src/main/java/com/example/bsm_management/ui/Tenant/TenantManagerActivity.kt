package com.example.bsm_management.ui.tenant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.FragmentTenantListBinding
import com.example.bsm_management.ui.excel.ExcelViewerActivity
import com.example.bsm_management.utils.ExcelExporter
import database.DatabaseHelper

class TenantManagerActivity : AppCompatActivity() {

    private lateinit var vb: FragmentTenantListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: TenantListAdapter

    private var allTenants: List<Tenant> = emptyList()
    private var selectedRoomId: Int? = null
    sealed class TenantRow {
        data class RoomHeader(val roomName: String) : TenantRow()
        data class TenantItem(val tenant: Tenant) : TenantRow()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vb = FragmentTenantListBinding.inflate(layoutInflater)
        setContentView(vb.root)
        ViewCompat.setOnApplyWindowInsetsListener(vb.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        db = DatabaseHelper(this)
        vb.btnBack.setOnClickListener { finish() }
        setupRoomDropdown()
        setupSearch()
        setupButtons()

        loadTenants()
    }
    override fun onResume() {
        super.onResume()
        loadTenants()   // load lại mỗi khi quay trở lại màn hình
    }

    private fun setupRoomDropdown() {
        val rooms = db.getAllRooms()
        val names = rooms.map { it.name }

        val dropAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            names
        )
        vb.spRoom.setAdapter(dropAdapter)

        vb.spRoom.setOnItemClickListener { _, _, pos, _ ->
            selectedRoomId = rooms[pos].id.toInt()
            applyFilter()
        }
    }

    private fun setupSearch() {
        vb.edtSearch.addTextChangedListener {
            applyFilter()
        }
    }

    private fun setupButtons() {
        vb.btnExport.setOnClickListener {

            val uri = ExcelExporter.exportTenants(this, allTenants)

            if (uri == null) {
                Toast.makeText(this, "Xuất Excel thất bại!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ExcelViewerActivity::class.java)
            intent.putExtra("fileUri", uri.toString())
            startActivity(intent)
        }



        vb.btnOldTenant.setOnClickListener {
            startActivity(Intent(this, OldTenantActivity::class.java))
        }
    }

    private fun loadTenants() {
        allTenants = db.getAllTenantsActive()
        setupRecycler()
        applyFilter()
    }

    private fun setupRecycler() {
        adapter = TenantListAdapter(
            onCall = { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            },
            onMore = { tenant ->
                TenantMenuSheet(
                    tenant,
                    onTenantUpdated = { loadTenants() },
                    onTenantDeleted = { loadTenants() }
                ).show(supportFragmentManager, "tenantMenu")
            }
        )

        vb.rvTenants.layoutManager = LinearLayoutManager(this)
        vb.rvTenants.adapter = adapter
    }

    private fun applyFilter() {
        var list = allTenants

        // Lọc theo phòng nếu chọn
        selectedRoomId?.let { rid ->
            list = list.filter { it.roomId == rid }
        }

        // Lọc theo tên/sđt
        val q = vb.edtSearch.text.toString()
        if (q.isNotEmpty()) {
            list = list.filter { it.name.contains(q, true) || it.phone.contains(q) }
        }

        // === NHÓM THEO PHÒNG ===
        val grouped = list.groupBy { it.roomId }

        val finalList = mutableListOf<TenantRow>()

        for ((roomId, tenants) in grouped) {
            val roomName = db.getRoomNameById(roomId)  // bạn thêm hàm này trong DB
            finalList.add(TenantRow.RoomHeader(roomName))
            tenants.forEach { t ->
                finalList.add(TenantRow.TenantItem(t))
            }
        }

        adapter.submitList(finalList)
        vb.tvCount.text = "Tổng có (${list.size}) khách"
    }

}
