package com.example.bsm_management.ui.tenant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.ActivityTenantListBinding
import database.DatabaseHelper

class OldTenantActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTenantListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: TenantListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vb = ActivityTenantListBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.tvTitle.text = "Khách thuê cũ"
        vb.btnBack.setOnClickListener { finish() }

        db = DatabaseHelper(this)

        setupRecycler()
        loadOldTenants()
    }

    private fun setupRecycler() {
        adapter = TenantListAdapter(
            onCall = { phone ->
                // gọi điện
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:$phone")
                )
                startActivity(intent)
            },
            onMore = { tenant ->
                // không cho sửa nữa – chỉ cho xóa vĩnh viễn
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa vĩnh viễn")
                    .setMessage("Bạn chắc chắn muốn xóa ${tenant.name}?")
                    .setPositiveButton("Xóa") { _, _ ->
                        db.writableDatabase.delete("tenants", "id=?", arrayOf(tenant.id.toString()))
                        loadOldTenants()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )

        vb.rvTenants.layoutManager = LinearLayoutManager(this)
        vb.rvTenants.adapter = adapter
    }

    private fun loadOldTenants() {
        val list = db.getOldTenants()
        adapter.submitList(list)
        vb.tvCount.text = "Tổng có ${list.size} khách cũ"
    }
}
