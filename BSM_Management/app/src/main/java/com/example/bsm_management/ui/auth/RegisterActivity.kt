package com.example.bsm_management.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.bsm_management.R
import database.DatabaseHelper
import kotlinx.coroutines.*
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var tvGoLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Edge-to-edge padding
        val root = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Ánh xạ view
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvGoLogin = findViewById(R.id.tvGoLogin)

        // Tự động điền cache nếu người dùng nhập lỗi trước đó
        readRegisterCache()?.let { (name, phone) ->
            etFullName.setText(name)
            etPhone.setText(phone)
        }

        // Nút back
        btnBack.setOnClickListener { finish() }

        // Chuyển sang đăng nhập
        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Xử lý đăng ký
        btnRegister.setOnClickListener {
            attemptRegister()
        }
    }

    private fun attemptRegister() {
        val name = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        // Lưu cache phòng khi lỗi
        saveRegisterCache(name, phone)

        // Kiểm tra điều kiện
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 8) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirm) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            progressBar.visibility = android.view.View.VISIBLE
            val exists = withContext(Dispatchers.IO) { checkPhoneExists(phone) }
            progressBar.visibility = android.view.View.GONE

            if (exists) {
                Toast.makeText(this@RegisterActivity, "Số điện thoại đã được đăng ký!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            withContext(Dispatchers.IO) { insertUser(name, phone, password) }

            // Đăng ký thành công → Xóa cache
            deleteRegisterCache()
            Toast.makeText(this@RegisterActivity, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            finish()
        }
    }

    // ================== SQLite helper ==================
    private fun checkPhoneExists(phone: String): Boolean {
        val db = DatabaseHelper(applicationContext).readableDatabase
        db.rawQuery("SELECT id FROM users WHERE phone=?", arrayOf(phone)).use { c ->
            return c.moveToFirst()
        }
    }

    private fun insertUser(name: String, phone: String, password: String) {
        val db = DatabaseHelper(applicationContext).writableDatabase
        val values = android.content.ContentValues().apply {
            put("name", name)
            put("phone", phone)
            put("password", password)
        }
        db.insert("users", null, values)
        db.close()
    }

    // ================== Cache helper ==================
    private fun saveRegisterCache(name: String, phone: String) {
        val file = File(cacheDir, "register_temp.txt")
        file.writeText("$name|$phone")
    }

    private fun readRegisterCache(): Pair<String, String>? {
        val file = File(cacheDir, "register_temp.txt")
        return if (file.exists()) {
            val parts = file.readText().split("|")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        } else null
    }

    private fun deleteRegisterCache() {
        val file = File(cacheDir, "register_temp.txt")
        if (file.exists()) file.delete()
    }
}
