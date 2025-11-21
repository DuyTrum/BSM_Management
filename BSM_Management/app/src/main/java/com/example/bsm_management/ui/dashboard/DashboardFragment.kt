package com.example.bsm_management.ui.dashboard

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bsm_management.R
import com.example.bsm_management.databinding.FragmentDashboardBinding
import com.example.bsm_management.ui.MangeDashboard.DashboardManageFragment
import com.example.bsm_management.ui.overview.OverviewFragment
import com.example.bsm_management.ui.qr.QrScanActivity
import database.DatabaseHelper

class DashboardFragment : Fragment() {

    private var _vb: FragmentDashboardBinding? = null
    private val vb get() = _vb!!

    private fun openHostelPicker() {
        HostelListBottomSheet().show(parentFragmentManager, "HostelList")
    }

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentDashboardBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tab mặc định = Quản lý
        vb.toggleTabs.check(vb.btnManage.id)
        childFragmentManager.beginTransaction()
            .replace(R.id.tabContainer, DashboardManageFragment())
            .commitNow()

        // Đổi tab
        vb.toggleTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val target = when (checkedId) {
                vb.btnManage.id -> DashboardManageFragment()
                vb.btnOverview.id -> OverviewFragment()
                else -> DashboardManageFragment()
            }
            childFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.tabContainer, target)
                .commit()
        }

        // Menu / QR
        vb.btnMenu.setOnClickListener {
            DashboardMenuBottomSheet().show(parentFragmentManager, "DashboardMenu")
        }
        vb.btnQr.setOnClickListener {
            startActivity(Intent(requireContext(), QrScanActivity::class.java))
        }

        // Đổi nhà nhanh
        val open = View.OnClickListener { openHostelPicker() }
        vb.tvHostelName.setOnClickListener(open)
        vb.ivChevron.setOnClickListener(open)

        // Lần đầu vào: cập nhật header theo dữ liệu thật
        updateHeaderFromData()
    }

    override fun onResume() {
        super.onResume()
        // Khi vừa tạo nhà mới quay lại, refresh header
        updateHeaderFromData()
    }

    private fun updateHeaderFromData() {
        val ctx = requireContext()
        val prefs = requireContext().getSharedPreferences("hostel_prefs", 0)
        val name    = prefs.getString("hostel_name", null).orEmpty()


        val (total, occupied) = getRoomStats()
        val empty = (total - occupied).coerceAtLeast(0)

        vb.tvHostelName.text = name
        vb.tvSubtitle.text = "Tổng $total phòng • Trống $empty"
    }

    /** Lấy thống kê phòng từ bảng `rooms` */
    private fun getRoomStats(): Pair<Int, Int> {
        val db = DatabaseHelper(requireContext()).readableDatabase
        var total = 0
        var occupied = 0

        fun Cursor.firstIntOrZero(): Int =
            if (moveToFirst()) getInt(0) else 0

        db.rawQuery("SELECT COUNT(*) FROM rooms", null).use { total = it.firstIntOrZero() }
        db.rawQuery("SELECT COUNT(*) FROM rooms WHERE status='OCCUPIED'", null).use {
            occupied = it.firstIntOrZero()
        }
        return total to occupied
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}
