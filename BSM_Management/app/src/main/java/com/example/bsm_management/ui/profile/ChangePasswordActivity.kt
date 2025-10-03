package com.example.bsm_management.ui.profile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // trong onCreate của Activity
        val header = findViewById<View>(R.id.header)
        header.findViewById<TextView>(R.id.tvHeaderTitle).text = "Đổi mật khẩu"
        header.findViewById<TextView>(R.id.tvHeaderSubtitle).text = "Nhập và xác nhận mật khẩu mới"

        val ivBack = header.findViewById<ImageView>(R.id.ivBack)
        ivBack.isClickable = true
        ivBack.isFocusable = true
        ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // hoặc finish()
        }

    }
}