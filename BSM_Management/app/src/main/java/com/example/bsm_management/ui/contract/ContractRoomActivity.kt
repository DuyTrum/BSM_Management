package com.example.bsm_management.ui.contract

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R

class RoomContractActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_contract)

        // 1) Áp insets: cần root id=main
        findViewById<View>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        // 2) Gán click bằng code (khỏi phụ thuộc android:onClick)
        findViewById<View>(R.id.cardRoom1)?.setOnClickListener { onClickRoomCard(it) }
    }

    fun onClickRoomCard(v: View) {
        Toast.makeText(this, "Click card!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, AddContractActivity::class.java))
    }
}

