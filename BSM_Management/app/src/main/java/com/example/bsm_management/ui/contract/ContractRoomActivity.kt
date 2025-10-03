package com.example.bsm_management.ui.contract

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class ContractRoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_contract)

        // Fit system bars cho root id=main
        findViewById<View>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        val rv = findViewById<RecyclerView>(R.id.rvRooms)
        rv.layoutManager = LinearLayoutManager(this)

        // Demo list phòng
        val rooms = listOf(
            ContractRoomItem("Phòng 101", "3.000.000 đ", isEmpty = true),
            ContractRoomItem("Phòng 102", "3.200.000 đ", waitNextCycle = true),
            ContractRoomItem("Phòng 201", "2.800.000 đ"),
            ContractRoomItem("Phòng 202", "3.000.000 đ", isEmpty = true, waitNextCycle = true)
        )

        rv.adapter = ContractRoomAdapter(rooms) { item ->
            Toast.makeText(this, "Chọn ${item.roomName}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AddContractActivity::class.java))
        }
    }
}
