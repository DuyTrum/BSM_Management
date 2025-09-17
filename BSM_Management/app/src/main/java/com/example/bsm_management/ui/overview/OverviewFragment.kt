package com.example.bsm_management.ui.overview

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class OverviewFragment : Fragment(R.layout.fragmen_overview) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvOverview)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        val ad = OverviewAdapter()
        rv.adapter = ad

        ad.submitList(
            listOf(
                OverviewStat("Số phòng có thể cho thuê", 0, 0, R.drawable.ic_cart, Color.parseColor("#FFF2EE")),
                OverviewStat("Số phòng đang trống", 5, 100, R.drawable.ic_box, Color.parseColor("#FFEDE8")),
                OverviewStat("Số phòng đang thuê", 0, 0, R.drawable.ic_cube, Color.parseColor("#EAF7EF")),
                OverviewStat("Sắp kết thúc hợp đồng", 0, 0, R.drawable.ic_warning, Color.parseColor("#FFF5E5")),
                OverviewStat("Báo kết thúc hợp đồng", 0, 0, R.drawable.ic_clipboard, Color.parseColor("#FFF5E5")),
                OverviewStat("Quá hạn hợp đồng", 0, 0, R.drawable.ic_clock, Color.parseColor("#F0F0F0")),
                OverviewStat("Đang nợ tiền", 0, 0, R.drawable.ic_attach_money, Color.parseColor("#EAF6EE")),
                OverviewStat("Đang cọc giữ chỗ", 0, 0, R.drawable.ic_anchor, Color.parseColor("#EDF1F6"))
            )
        )
    }
}
