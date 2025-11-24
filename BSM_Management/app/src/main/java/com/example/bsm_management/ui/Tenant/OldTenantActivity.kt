package com.example.bsm_management.ui.tenant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.ActivityTenantListBinding
import database.DatabaseHelper

class OldTenantActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTenantListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: TenantListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        vb = ActivityTenantListBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.tvTitle.text = "Khách thuê cũ"
        vb.btnBack.setOnClickListener { finish() }
        vb.tvRoomName.text = ""
        vb.tvRoomName.visibility = View.GONE

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

                val options = arrayOf("Khôi phục khách thuê", "Xóa vĩnh viễn")

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(tenant.name)
                    .setItems(options) { _, which ->
                        when (which) {

                            // ========================
                            // 1️⃣ KHÔI PHỤC
                            // ========================
                            0 -> {
                                val ok = db.restoreOldTenant(this, tenant.id)
                                if (ok) {
                                    Toast.makeText(this, "Khôi phục thành công!", Toast.LENGTH_SHORT).show()
                                    loadOldTenants()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Không thể khôi phục (phòng đã đầy hoặc đã bị xoá)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            // ========================
                            // 2️⃣ XOÁ VĨNH VIỄN
                            // ========================
                            1 -> {
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Xóa vĩnh viễn")
                                    .setMessage("Bạn chắc chắn muốn xoá ${tenant.name}?")
                                    .setPositiveButton("Xóa") { _, _ ->
                                        db.writableDatabase.delete(
                                            "tenants", "id=?",
                                            arrayOf(tenant.id.toString())
                                        )
                                        loadOldTenants()
                                    }
                                    .setNegativeButton("Hủy", null)
                                    .show()
                            }
                        }
                    }
                    .show()
            }

        )

        vb.rvTenants.layoutManager = LinearLayoutManager(this)
        vb.rvTenants.adapter = adapter
    }

    private fun loadOldTenants() {
        val list = db.getOldTenants()
        val rows = list.map { TenantManagerActivity.TenantRow.TenantItem(it) }
        adapter.submitList(rows)
        vb.tvCount.text = "Tổng ${list.size} khách cũ"
    }

}
