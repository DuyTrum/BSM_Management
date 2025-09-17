package com.example.bsm_management.ui.MangeDashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.ui.dashboard.ActionAdapter
import com.example.bsm_management.ui.dashboard.ActionItem
import com.example.bsm_management.ui.dashboard.GridSpacingItemDecoration

class DashboardManageFragment : Fragment(R.layout.fragment_dashboard_manage) {

    private val adapter = ActionAdapter { item ->
        // TODO: handle click action
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // RV 3 cột
        val rv = view.findViewById<RecyclerView>(R.id.rvActions)
        val span = 3
        rv.layoutManager = GridLayoutManager(requireContext(), span)
        val spacing = resources.getDimensionPixelSize(R.dimen.pad_m) // ~12–16dp
        rv.addItemDecoration(GridSpacingItemDecoration(span, spacing, includeEdge = true))
        rv.adapter = adapter

        adapter.submitList(
            listOf(
                ActionItem(R.drawable.ic_handshake, "Cọc giữ chỗ"),
                ActionItem(R.drawable.ic_people_swap, "Lập hợp đồng mới", badge = 5),
                ActionItem(R.drawable.ic_exit_person, "Thanh lý\n(Trả phòng)"),
                ActionItem(R.drawable.ic_receipt_long, "Lập hóa đơn"),
                ActionItem(R.drawable.ic_calc_receipt, "Chốt & Lập\nhóa đơn"),
                ActionItem(R.drawable.ic_bill_due, "Hóa đơn\ncần thu tiền")
            )
        )

        view.findViewById<View>(R.id.btnAllowNotification)?.setOnClickListener {
            // TODO: mở Settings/Notification hoặc flow cấp quyền
            Toast.makeText(requireContext(), "Cho phép thông báo", Toast.LENGTH_SHORT).show()
        }
    }
}