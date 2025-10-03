package com.example.bsm_management.ui.MangeDashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.ui.contract.ContractListActivity
import com.example.bsm_management.ui.contract.ContractRoomActivity
import com.example.bsm_management.ui.dashboard.ActionAdapter
import com.example.bsm_management.ui.dashboard.ActionItem
import com.example.bsm_management.ui.dashboard.GridSpacingItemDecoration
import com.example.bsm_management.ui.invoice.AddInvoiceActivity
import com.example.bsm_management.ui.invoice.InvoiceActivity
import com.example.bsm_management.ui.invoice.InvoiceListActivity

class DashboardManageFragment : Fragment(R.layout.fragment_dashboard_manage) {

    private val adapter = ActionAdapter { item ->
        when (item.title) {
            "Lập hợp đồng" -> startActivity(Intent(requireContext(), ContractRoomActivity::class.java))
            "Lập hóa đơn" -> startActivity(Intent(requireContext(), InvoiceActivity::class.java))
            "Quản lý hợp đồng" -> startActivity(Intent(requireContext(), ContractListActivity::class.java))
            "Quản lý hóa đơn" -> startActivity(Intent(requireContext(), InvoiceListActivity::class.java))
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvActions)
        val span = 2 // 2 hoặc 3 tùy bạn
        rv.layoutManager = GridLayoutManager(requireContext(), span)
        val spacing = resources.getDimensionPixelSize(R.dimen.pad_m)
        rv.addItemDecoration(GridSpacingItemDecoration(span, spacing, includeEdge = true))
        rv.adapter = adapter

        adapter.submitList(
            listOf(
                ActionItem(R.drawable.ic_handshake,   "Lập hợp đồng"),
                ActionItem(R.drawable.ic_receipt_long,"Lập hóa đơn"),
                ActionItem(R.drawable.ic_contract,    "Quản lý hợp đồng"),
                ActionItem(R.drawable.ic_bill_due,    "Quản lý hóa đơn"),
            )
        )
    }
}
