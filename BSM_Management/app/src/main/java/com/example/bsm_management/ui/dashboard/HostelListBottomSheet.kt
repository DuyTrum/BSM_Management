package com.example.bsm_management.ui.dashboard

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.bsm_management.R
import com.example.bsm_management.ui.intro.EmptyStateActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import database.DatabaseHelper

class HostelListBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_hostel_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- bind views
        val tvName     = view.findViewById<TextView>(R.id.tvHostelName)
        val tvAddress  = view.findViewById<TextView>(R.id.tvHostelAddress)
        val tvRooms    = view.findViewById<TextView>(R.id.tvRoomCount)
        val btnDelete  = view.findViewById<MaterialButton>(R.id.btnDelete)
        val btnManage  = view.findViewById<MaterialButton>(R.id.btnManage)

        // --- load dữ liệu đã lưu ở AddHostelActivity (SharedPreferences)
        val pref  = requireContext().getSharedPreferences("hostel_prefs", 0)
        val name  = pref.getString("hostel_name", "") ?: ""
        val addr  = pref.getString("hostel_address", "") ?: ""

        tvName.text    = if (name.isNotBlank()) "Nhà trọ $name" else "Nhà trọ chưa đặt tên"
        tvAddress.text = if (addr.isNotBlank()) addr else "Chưa có địa chỉ"

        // --- đếm số phòng thật trong DB
        tvRooms.text = "${countRooms()} phòng cho thuê"

        // --- actions
        btnDelete.setOnClickListener { confirmAndDeleteAll() }
        btnManage.setOnClickListener { dismiss() } // đóng sheet, ở lại màn hiện tại
    }

    /** Đếm số phòng trong bảng rooms */
    private fun countRooms(): Int {
        return try {
            val db = DatabaseHelper(requireContext()).readableDatabase
            var c: Cursor? = null
            var count = 0
            try {
                c = db.rawQuery("SELECT COUNT(*) FROM rooms", null)
                if (c.moveToFirst()) count = c.getInt(0)
            } finally {
                c?.close()
                db.close()
            }
            count
        } catch (_: Exception) {
            0
        }
    }

    private fun confirmAndDeleteAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa nhà?")
            .setMessage(
                "Thao tác này sẽ xóa TẤT CẢ phòng và dữ liệu liên quan (hợp đồng, hóa đơn)." +
                        "\nBạn có chắc muốn tiếp tục?"
            )
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                val db = DatabaseHelper(requireContext())

                // Gọi hàm xóa cascade trong DatabaseHelper
                val deletedCount = db.deleteAllRoomsCascade()

                // Xóa luôn SharedPreferences hiện tại (tên, địa chỉ, v.v.)
                requireContext().getSharedPreferences("hostel_prefs", 0)
                    .edit().clear().apply()

                Toast.makeText(
                    requireContext(),
                    "Đã xóa $deletedCount phòng và dữ liệu liên quan.",
                    Toast.LENGTH_LONG
                ).show()

                // Điều hướng về màn hình rỗng (EmptyStateActivity)
                startActivity(
                    Intent(requireContext(), EmptyStateActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                dismiss()
                requireActivity().finish()
            }
            .show()
    }
}
