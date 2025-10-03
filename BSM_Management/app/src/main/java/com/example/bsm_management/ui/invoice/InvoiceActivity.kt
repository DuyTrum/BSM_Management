package com.example.bsm_management.ui.invoice

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class InvoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_in_voice)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Header
        findViewById<TextView>(R.id.tvHeaderTitle).text = "Lập hóa đơn"
        findViewById<TextView>(R.id.tvHeaderSubtitle).text = "Chọn 1 phòng để lập hóa đơn"
        val back: View = findViewById(R.id.ivBack)
        back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvInvoices)
        val adapter = InvoiceAdapter { item ->
            Toast.makeText(this, "Click ${item.roomName}", Toast.LENGTH_SHORT).show()
        }
        rv.adapter = adapter  // layoutManager đã set trong XML

        // Demo data
        adapter.submitList(
            listOf(
                RoomItem(
                    "Phòng 1", "Thbb - 0958587658",
                    "07/09/2025 - Vô thời hạn",
                    "Đang ở", "3.000.000đ", "1/1 người"
                ),
                RoomItem(
                    "Phòng 2", "Nam - 0912345678",
                    "01/01/2025 - 01/01/2026",
                    "Chờ kỳ thu tới", "2.800.000đ", "2/2 người"
                ),
                RoomItem(
                    "Phòng 3", "Nam - 0912745678",
                    "01/01/2025 - 01/01/2026",
                    "Chờ kỳ thu tới", "2.800.000đ", "2/2 người"
                )
            )
        )
    }
}
