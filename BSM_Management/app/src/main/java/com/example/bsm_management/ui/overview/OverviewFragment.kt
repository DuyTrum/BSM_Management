package com.example.bsm_management.ui.overview

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.ui.contract.ContractListActivity
import com.example.bsm_management.ui.room.RoomListActivity
import database.DatabaseHelper

class OverviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmen_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvOverview)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        val ad = OverviewAdapter()
        rv.adapter = ad

        val db = DatabaseHelper(requireContext())

        val totalRooms = db.countRooms().toInt()
        val available = db.countAvailableRooms()
        val empty = db.countAvailableRooms()
        val renting = db.countRentingRooms()

        val endingSoon = db.countContractsEndingSoon(7)
        val notified = db.countContractsNotified()
        val overdue = db.countContractsOverdue()

        ad.submitList(
            listOf(
                OverviewStat("Số phòng có thể cho thuê", available, percent(available, totalRooms), R.drawable.ic_cart, 0xFFF2EE),
                OverviewStat("Số phòng đang trống", empty, percent(empty, totalRooms), R.drawable.ic_box, 0xFFEDE8),
                OverviewStat("Số phòng đang thuê", renting, percent(renting, totalRooms), R.drawable.ic_cube, 0xEAF7EF),
                OverviewStat("Sắp kết thúc hợp đồng", endingSoon, percent(endingSoon, totalRooms), R.drawable.ic_warning, 0xFFF5E5),
                OverviewStat("Báo kết thúc hợp đồng", notified, percent(notified, totalRooms), R.drawable.ic_clipboard, 0xFFF5E5),
                OverviewStat("Quá hạn hợp đồng", overdue, percent(overdue, totalRooms), R.drawable.ic_clock, 0xF0F0F0),
            )
        )
    }
    private fun percent(value: Int, total: Int): Int {
        if (total == 0) return 0
        return value * 100 / total
    }
    private fun handleStatClick(stat: OverviewStat) {

        when (stat.title) {

            // 1) Phòng có thể cho thuê
            "Số phòng có thể cho thuê",
            "Số phòng đang trống" -> {
                val intent = Intent(requireContext(), RoomListActivity::class.java)
                intent.putExtra("filter_status", "EMPTY")   // bạn đổi nếu cần
                startActivity(intent)
            }

            // 2) Phòng đang thuê
            "Số phòng đang thuê" -> {
                val intent = Intent(requireContext(), RoomListActivity::class.java)
                intent.putExtra("filter_status", "RENTED")
                startActivity(intent)
            }

            // 3) Sắp hết hạn hợp đồng
            "Sắp kết thúc hợp đồng" -> {
                val intent = Intent(requireContext(),   ContractListActivity::class.java)
                intent.putExtra("filter_mode", "ENDING_SOON")
                startActivity(intent)
            }

            // 4) Báo kết thúc hợp đồng
            "Báo kết thúc hợp đồng" -> {
                val intent = Intent(requireContext(), ContractListActivity::class.java)
                intent.putExtra("filter_mode", "NOTIFIED")
                startActivity(intent)
            }

            // 5) Quá hạn hợp đồng
            "Quá hạn hợp đồng" -> {
                val intent = Intent(requireContext(), ContractListActivity::class.java)
                intent.putExtra("filter_mode", "OVERDUE")
                startActivity(intent)
            }
        }
    }



}

