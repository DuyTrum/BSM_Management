// file: app/src/main/java/.../ui/contract/RoomContractActivity.kt
package com.example.bsm_management.ui.contract

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R

class RoomContractActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_contract)
    }

    // Handler cho android:onClick ở XML
    fun onClickRoomCard(v: View) {
        // Test xem click có vào chưa
         Toast.makeText(this, "Click card!", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, AddContractActivity::class.java))
    }
}
