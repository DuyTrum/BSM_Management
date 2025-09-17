package com.example.bsm_management.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity   // ✅ thêm dòng này

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

        // Ánh xạ view
        btnBack = findViewById(R.id.btnBack)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)

        // Event: nút quay lại
        btnBack.setOnClickListener { _: View ->
            finish()
        }

        // Event: nút đăng nhập
        btnLogin.setOnClickListener { _: View ->
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
            } else if (phone == "0123456789" && password == "123456") {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Số điện thoại hoặc mật khẩu sai", Toast.LENGTH_SHORT).show()
            }
        }

        // Event: quên mật khẩu
        tvForgotPassword.setOnClickListener { _: View ->
            Toast.makeText(this, "Chức năng quên mật khẩu", Toast.LENGTH_SHORT).show()
        }

        // Event: đăng ký
        tvRegister.setOnClickListener { _: View ->
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}