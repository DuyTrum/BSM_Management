package com.example.bsm_management.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bsm_management.R
import com.example.bsm_management.databinding.FragmentDashboardBinding
import com.example.bsm_management.ui.MangeDashboard.DashboardManageFragment
import com.example.bsm_management.ui.overview.OverviewFragment
import com.example.bsm_management.ui.qr.QrScanActivity

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

        // Chọn mặc định “Quản lý”
        vb.toggleTabs.check(vb.btnManage.id)
        childFragmentManager.beginTransaction()
            .replace(R.id.tabContainer, DashboardManageFragment())
            .commitNow()

        // Lắng nghe đổi tab
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
        vb.btnMenu.setOnClickListener {
            val sheet = DashboardMenuBottomSheet()
            sheet.show(parentFragmentManager, "DashboardMenu")
        }
        vb.btnQr.setOnClickListener {
            val intent = Intent(requireContext(), QrScanActivity::class.java)
            startActivity(intent)
        }
        val open = View.OnClickListener { openHostelPicker() }
        vb.tvHostelName.setOnClickListener(open)
        vb.ivChevron.setOnClickListener(open)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}
