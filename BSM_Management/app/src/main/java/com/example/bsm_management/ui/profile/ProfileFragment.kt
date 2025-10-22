package com.example.bsm_management.ui.profile

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bsm_management.R
import com.example.bsm_management.ui.auth.LoginActivity
import database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val prefs by lazy { requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvHello       = view.findViewById<TextView>(R.id.tvHello)
        val tvGreet       = view.findViewById<TextView>(R.id.tvGreetingMessage)
        val tvMaKH        = view.findViewById<TextView>(R.id.tvMaKhachHang)
        val tvPhone       = view.findViewById<TextView>(R.id.tvPhone)
        val tvJoinedDate  = view.findViewById<TextView>(R.id.tvJoinedDate)
        val btnCopy       = view.findViewById<LinearLayout>(R.id.btnCopy)
        val rowCompany    = view.findViewById<LinearLayout>(R.id.rowCompany)
        val rowChangePass = view.findViewById<LinearLayout>(R.id.rowChangePassword)
        val rowRate       = view.findViewById<LinearLayout>(R.id.rowAppPermission)
        val rowHelp       = view.findViewById<LinearLayout>(R.id.rowHelp)
        val btnLogout     = view.findViewById<LinearLayout>(R.id.btnLogout)
        val ivAvatar      = view.findViewById<ImageView>(R.id.ivAvatar)

        // ===== Load user theo userId đã lưu sau đăng nhập =====
        val (name, phone) = loadUserFromDbBySession() ?: ("Bạn" to "+84 ")
        tvHello.text = "Xin chào! $name"
        tvGreet.text = "Chúc bạn một ngày làm việc hiệu quả"
        tvMaKH.text  = "#BSM-${name.take(3).uppercase(Locale.getDefault())}001"
        tvPhone.text = phone
        tvJoinedDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 90L * 24 * 3600 * 1000)) // giả lập "tham gia 3 tháng"

        // Copy mã KH
        btnCopy.setOnClickListener {
            val code = tvMaKH.text?.toString().orEmpty()
            if (code.isBlank()) { toast("Không có mã để sao chép"); return@setOnClickListener }
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Mã khách hàng", code))
            toast("Đã sao chép: $code")
        }

        // Gọi điện khi chạm số
        tvPhone.setOnClickListener {
            val p = tvPhone.text?.toString()?.trim().orEmpty()
            if (p.isBlank()) { toast("Chưa có số điện thoại"); return@setOnClickListener }
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p")))
        }

        rowCompany.setOnClickListener { toast("Tính năng đang phát triển") }
        rowChangePass.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
        rowRate.setOnClickListener { openStoreToRate() }
        rowHelp.setOnClickListener {
            val it = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@bsm.app")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ phần mềm BSM")
                putExtra(Intent.EXTRA_TEXT, "Mô tả vấn đề: \n\nThiết bị: ${deviceInfo()}")
            }
            val ok = runCatching { startActivity(it) }.isSuccess
            if (!ok) runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=BSM+support")))
            }.onFailure { toast("Không thể mở trung tâm trợ giúp") }
        }
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    prefs.edit().clear().apply()
                    val it = Intent(requireContext(), LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(it)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        ivAvatar.setOnClickListener {
            val options = arrayOf("Cài đặt thông báo", "Cài đặt báo thức chính xác")
            AlertDialog.Builder(requireContext())
                .setTitle("Tiện ích nhanh")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openNotificationSettings()
                        1 -> openExactAlarmSettings()
                    }
                }
                .show()
        }
    }

    /** Trả về Pair(name, phone) theo userId (đọc từ SharedPreferences). */
    private fun loadUserFromDbBySession(): Pair<String, String>? {
        val userId = prefs.getLong("userId", -1L)
        val db = DatabaseHelper(requireContext()).readableDatabase
        try {
            if (userId > 0) {
                db.rawQuery("SELECT name, phone FROM users WHERE id=? LIMIT 1",
                    arrayOf(userId.toString())
                ).use { c ->
                    if (c.moveToFirst()) return c.getString(0) to c.getString(1)
                }
            }
            // Fallback: nếu chưa có session (trường hợp test), lấy user đầu tiên
            db.rawQuery("SELECT name, phone FROM users ORDER BY id LIMIT 1", null).use { c ->
                if (c.moveToFirst()) return c.getString(0) to c.getString(1)
            }
        } finally { db.close() }
        return null
    }

    private fun openStoreToRate() {
        val appId = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId")))
        }
    }

    private fun openNotificationSettings() {
        val it = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        runCatching { startActivity(it) }.onFailure { toast("Không mở được cài đặt thông báo") }
    }

    private fun openExactAlarmSettings() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val it = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            runCatching { startActivity(it) }.onFailure { toast("Không mở được trang cấp quyền báo thức") }
        } else toast("Thiết bị không cần quyền này")
    }

    private fun deviceInfo(): String =
        "Model: ${android.os.Build.MODEL}, SDK: ${android.os.Build.VERSION.SDK_INT}"

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
