package com.example.bsm_management.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.widget.Toast

class DashboardMenuBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.bottom_sheet_dashboard, container, false)

        v.findViewById<View>(R.id.btnAddBuilding).setOnClickListener {
            Toast.makeText(requireContext(), "Thêm mới tòa nhà", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        v.findViewById<View>(R.id.btnEditHostel).setOnClickListener {
            Toast.makeText(requireContext(), "Chỉnh sửa nhà trọ", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        v.findViewById<View>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(requireContext(), "Cài đặt", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return v
    }
}
