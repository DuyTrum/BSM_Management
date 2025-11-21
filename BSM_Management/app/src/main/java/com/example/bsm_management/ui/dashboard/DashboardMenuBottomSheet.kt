package com.example.bsm_management.ui.dashboard

import EditHostelSheet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bsm_management.R
import com.example.bsm_management.ui.hostel.AddHostelActivity
import com.example.bsm_management.ui.main.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DashboardMenuBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme  // đẹp như BSM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.bottom_sheet_dashboard, container, false)

        // ===== 1. THÊM MỚI TÒA NHÀ =====
        v.findViewById<View>(R.id.btnAddBuilding).setOnClickListener {
            startActivity(Intent(requireContext(), AddHostelActivity::class.java))
            dismiss()
        }

        // ===== 2. CHỈNH SỬA THÔNG TIN NHÀ TRỌ =====
        v.findViewById<View>(R.id.btnEditHostel).setOnClickListener {
            EditHostelSheet().show(parentFragmentManager, "edit_hostel")
            dismiss()
        }

        v.findViewById<View>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.putExtra("open_profile", true)
            startActivity(intent)
            dismiss()
        }


        return v
    }
}
