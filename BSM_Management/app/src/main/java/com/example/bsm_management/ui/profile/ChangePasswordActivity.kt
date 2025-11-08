package com.example.bsm_management.ui.profile

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var etOld: EditText
    private lateinit var etNew: EditText
    private lateinit var etConfirm: EditText
    private lateinit var btnSave: Button

    private val prefs by lazy { getSharedPreferences("auth", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Header
        val header = findViewById<View>(R.id.header)
        header.findViewById<TextView>(R.id.tvHeaderTitle).text = "Đổi mật khẩu"
        header.findViewById<TextView>(R.id.tvHeaderSubtitle).text = "Nhập và xác nhận mật khẩu mới"
        header.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Views
        etOld     = findViewById(R.id.etOld)
        etNew     = findViewById(R.id.etNew)
        etConfirm = findViewById(R.id.etConfirm)
        btnSave   = findViewById(R.id.btnSave)

        btnSave.setOnClickListener { handleChangePassword() }
    }

    private fun handleChangePassword() {
        val oldPwd = etOld.text?.toString()?.trim().orEmpty()
        val newPwd = etNew.text?.toString()?.trim().orEmpty()
        val cfmPwd = etConfirm.text?.toString()?.trim().orEmpty()

        if (oldPwd.isEmpty()) { etOld.error = "Nhập mật khẩu hiện tại"; etOld.requestFocus(); return }
        if (newPwd.isEmpty()) { etNew.error = "Nhập mật khẩu mới"; etNew.requestFocus(); return }
        if (newPwd.length < 9) { etNew.error = "Mật khẩu phải > 8 ký tự"; etNew.requestFocus(); return }
        if (cfmPwd.isEmpty()) { etConfirm.error = "Xác nhận mật khẩu mới"; etConfirm.requestFocus(); return }
        if (newPwd != cfmPwd) { etConfirm.error = "Xác nhận không khớp"; etConfirm.requestFocus(); return }
        if (newPwd == oldPwd) { etNew.error = "Mật khẩu mới phải khác mật khẩu cũ"; etNew.requestFocus(); return }

        val userId = getCurrentUserId()        // <— dùng long
        if (userId <= 0L) { toast("Không xác định được người dùng hiện tại"); return }

        val db = DatabaseHelper(this).writableDatabase
        val okOld = db.rawQuery(
            "SELECT id FROM users WHERE id=? AND password=? LIMIT 1",
            arrayOf(userId.toString(), oldPwd)
        ).use { c -> c.moveToFirst() }

        if (!okOld) {
            etOld.error = "Mật khẩu hiện tại không đúng"
            etOld.requestFocus()
            db.close()
            return
        }

        val cv = android.content.ContentValues().apply { put("password", newPwd) }
        val rows = db.update("users", cv, "id=?", arrayOf(userId.toString()))
        db.close()

        if (rows > 0) {
            Toast.makeText(this, "Đã đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentUserId(): Long {
        val prefId = prefs.getLong("userId", -1L)
        if (prefId > 0L) return prefId

        // fallback: lấy user đầu tiên
        val db = database.DatabaseHelper(this).readableDatabase
        val id = db.rawQuery("SELECT id FROM users ORDER BY id LIMIT 1", null).use { c ->
            if (c.moveToFirst()) c.getLong(0) else -1L
        }
        db.close()
        return id
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
