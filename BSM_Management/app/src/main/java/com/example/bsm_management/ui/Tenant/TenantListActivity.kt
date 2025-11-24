package com.example.bsm_management.ui.tenant

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.ActivityTenantListBinding
import database.DatabaseHelper
import kotlin.collections.filterNotNull

class TenantListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTenantListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: TenantSlotAdapter

    private var roomId: Int = 0
    private var roomName: String = ""
    private var maxPeople: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTenantListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }


        db = DatabaseHelper(this)

        // nhận dữ liệu từ màn hình phòng
        roomId = intent.getIntExtra("roomId", 0)
        roomName = intent.getStringExtra("roomName") ?: ""
        maxPeople = intent.getIntExtra("maxPeople", 1)

        // set title
        binding.tvRoomName.text = "$roomName — Tối đa: $maxPeople người"

        binding.btnBack.setOnClickListener { finish() }

        loadData()
    }

    private fun loadData() {
        val slots = db.getTenantSlots(roomId, maxPeople)
        val active = slots.count { it != null }
        adapter = TenantSlotAdapter(
            maxPeople = maxPeople,
            tenants = slots,
            onAddTenant = { slotIndex ->
                showAddTenantDialog(slotIndex)
            },
            onEditTenant = { tenant ->
                showEditTenantDialog(tenant)
            },
            onDeleteTenant = { tenant ->
                deleteTenant(tenant)
            }
        )


        binding.rvTenants.layoutManager = LinearLayoutManager(this)
        binding.rvTenants.adapter = adapter

        binding.tvCount.text = "Đang có $active khách"
    }

    private fun showAddTenantDialog(slot: Int) {
        val dialog = AddTenantDialog(
            context = this,
            onSave = { name, phone, cccd, address ->
                db.addTenant(
                    roomId = roomId,
                    name = name,
                    phone = phone,
                    cccd = cccd,
                    address = address,
                    dob = null,
                    slotIndex = slot
                )
                loadData()
            }

        )
        dialog.show()
    }

    private fun showEditTenantDialog(t: Tenant) {
        val dialog = EditTenantDialog(
            context = this,
            tenant = t,
            onSave = { updatedTenant ->
                db.updateTenant(updatedTenant)
                loadData()
            }
        )
        dialog.show()
    }


    private fun deleteTenant(t: Tenant) {
        db.moveTenantToOld(this, t)
        loadData()
    }

}
