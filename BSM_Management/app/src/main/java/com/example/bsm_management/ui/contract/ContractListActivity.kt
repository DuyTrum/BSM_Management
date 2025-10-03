package com.example.bsm_management.ui.contract

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class ContractListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contract_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        val rv = findViewById<RecyclerView>(R.id.rvContracts)
        rv.layoutManager = LinearLayoutManager(this)

        // Demo data
        val list = listOf(
            ContractListItem("Phòng 1", "45600", "Trong thời hạn hợp đồng",
                "3.000.000đ", "3.000.000đ", "Chưa thu", "07/09/2025", "07/09/2025", "Vô thời hạn"),
            ContractListItem("Phòng 2", "78901", "Sắp hết hạn",
                "2.500.000đ", "2.500.000đ", "Đã thu", "01/01/2025", "02/01/2025", "01/01/2026")
        )

        rv.adapter = ContractListAdapter(list)
    }
}
