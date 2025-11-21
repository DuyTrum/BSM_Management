package com.example.bsm_management.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity
import database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var cbRemember: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Xử lý Insets cho giao diện
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
        cbRemember = findViewById(R.id.cbRemember)

        // Bỏ qua màn hình đăng nhập nếu đã có session
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1L)

        if (userId != -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }


        // Tự động điền thông tin từ file Internal Storage
        readLoginInfo()?.let { (savedPhone, savedPassword) ->
            etPhone.setText(savedPhone)
            etPassword.setText(savedPassword)
            cbRemember.isChecked = true
            Toast.makeText(this, "Tự động điền thông tin đăng nhập", Toast.LENGTH_SHORT).show()
        }

        // Sự kiện nút quay lại
        btnBack.setOnClickListener { finish() }

        // Sự kiện đăng nhập
        btnLogin.setOnClickListener { attemptLogin() }

        // Chức năng khác
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

        lifecycleScope.launch {
            btnLogin.isEnabled = false
            val userId = withContext(Dispatchers.IO) { queryUserId(phone, password) }
            btnLogin.isEnabled = true

            if (userId != null) {
                saveSession(userId, phone)

                // Lưu vào Internal Storage nếu người dùng chọn "Ghi nhớ"
                if (cbRemember.isChecked) saveLoginInfo(phone, password)
                else deleteFile("login_cache.txt") // bỏ lưu nếu không chọn

                Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                goToMain()
            } else {
                Toast.makeText(this@LoginActivity, "Số điện thoại hoặc mật khẩu sai", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun queryUserId(phone: String, pass: String): Long? {
        val db = DatabaseHelper(applicationContext).readableDatabase
        db.rawQuery(
            "SELECT id FROM users WHERE phone=? AND password=? LIMIT 1",
            arrayOf(phone, pass)
        ).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    private fun saveSession(userId: Long, phone: String) {
        getSharedPreferences("auth", MODE_PRIVATE).edit().apply {
            putLong("userId", userId)
            putString("phone", phone)
            apply()
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    // Ghi thông tin đăng nhập
    private fun saveLoginInfo(phone: String, password: String) {
        val data = "$phone|$password"
        openFileOutput("login_cache.txt", MODE_PRIVATE).use {
            it.write(data.toByteArray())
        }
    }

    // Đọc thông tin đăng nhập
    private fun readLoginInfo(): Pair<String, String>? {
        return try {
            val content = openFileInput("login_cache.txt").bufferedReader().use { it.readText() }
            val parts = content.split("|")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        } catch (e: Exception) {
            null
        }
    }
}
