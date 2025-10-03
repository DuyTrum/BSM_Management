package com.example.bsm_management.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity
import database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Ánh xạ view
        btnBack = findViewById(R.id.btnBack)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)

        btnBack.setOnClickListener { finish() }

        btnLogin.setOnClickListener { attemptLogin() }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Chức năng quên mật khẩu (đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ số điện thoại & mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        // Chạy query DB trên IO thread
        lifecycleScope.launch {
            btnLogin.isEnabled = false
            val userId = withContext(Dispatchers.IO) { queryUserId(phone, password) }
            btnLogin.isEnabled = true

            if (userId != null) {
                saveSession(userId)
                Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                goToMain()
            } else {
                Toast.makeText(this@LoginActivity, "Số điện thoại hoặc mật khẩu sai", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Trả về userId nếu đúng, ngược lại null
    private fun queryUserId(phone: String, pass: String): Long? {
        val db = DatabaseHelper(applicationContext).readableDatabase
        db.rawQuery(
            "SELECT id FROM users WHERE phone=? AND password=? LIMIT 1",
            arrayOf(phone, pass)
        ).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else null  
        }
    }

    private fun saveSession(userId: Long) {
        getSharedPreferences("auth", MODE_PRIVATE)
            .edit {
                putLong("userId", userId)
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
