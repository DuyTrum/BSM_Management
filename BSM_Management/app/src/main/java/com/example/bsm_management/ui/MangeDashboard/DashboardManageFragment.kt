package com.example.bsm_management.ui.MangeDashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.ui.contract.ContractListActivity
import com.example.bsm_management.ui.contract.ContractRoomActivity
import com.example.bsm_management.ui.dashboard.ActionAdapter
import com.example.bsm_management.ui.dashboard.ActionItem
import com.example.bsm_management.ui.dashboard.GridSpacingItemDecoration
import com.example.bsm_management.ui.invoice.InvoiceActivity
import com.example.bsm_management.ui.invoice.InvoiceListActivity
import com.google.android.material.button.MaterialButton
import com.example.bsm_management.ui.room.RoomListActivity

class DashboardManageFragment : Fragment(R.layout.fragment_dashboard_manage) {

    // --- Grid actions ---
    private val adapter = ActionAdapter { item ->
        when (item.title) {
            "Lập hợp đồng"      -> startActivity(Intent(requireContext(), ContractRoomActivity::class.java))
            "Lập hóa đơn"       -> startActivity(Intent(requireContext(), InvoiceActivity::class.java))
            "Quản lý hợp đồng"  -> startActivity(Intent(requireContext(), ContractListActivity::class.java))
            "Quản lý hóa đơn"   -> startActivity(Intent(requireContext(), InvoiceListActivity::class.java))
        }
    }

    // --- Launchers xin quyền / mở settings ---
    private val requestPostNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Dù user chọn gì, khi quay lại cứ re-check để ẩn/hiện thẻ
        updateWarnCardVisibility()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ========== Card "Cho phép nhận thông báo" ==========
        view.findViewById<MaterialButton>(R.id.btnAllowNotification).setOnClickListener {
            handleAllowNotificationClick()
        }
        updateWarnCardVisibility()

        // ========== Grid actions ==========
        val rv = view.findViewById<RecyclerView>(R.id.rvActions)
        val span = 2
        rv.layoutManager = GridLayoutManager(requireContext(), span)
        val spacing = resources.getDimensionPixelSize(R.dimen.pad_m)
        rv.addItemDecoration(GridSpacingItemDecoration(span, spacing, includeEdge = true))
        rv.adapter = adapter
        adapter.submitList(
            listOf(
                ActionItem(R.drawable.ic_handshake,    "Lập hợp đồng"),
                ActionItem(R.drawable.ic_receipt_long, "Lập hóa đơn"),
                ActionItem(R.drawable.ic_contract_manage,     "Quản lý hợp đồng"),
                ActionItem(R.drawable.ic_bill_due,     "Quản lý hóa đơn"),
            )
        )
        val rvMenu = view.findViewById<RecyclerView>(R.id.rvMenu)
        val menuAdapter = ActionAdapter { item ->
            when (item.title) {
                "Quản lý phòng" -> startActivity(Intent(requireContext(), RoomListActivity::class.java))
//                "Quản lý khách thuê" -> startActivity(Intent(requireContext(), TenantListActivity::class.java))
            }
        }
        rvMenu.layoutManager = GridLayoutManager(requireContext(), span)
        rvMenu.addItemDecoration(GridSpacingItemDecoration(span, 16, true))
        rvMenu.adapter = menuAdapter

        menuAdapter.submitList(
            listOf(
                ActionItem(R.drawable.ic_room_manage, "Quản lý phòng"),
                ActionItem(R.drawable.ic_tenant_manage, "Quản lý khách thuê"),
            )
        )
    }

    override fun onResume() {
        super.onResume()
        // Nếu người dùng vừa đi bật trong Settings quay lại -> cập nhật hiển thị
        updateWarnCardVisibility()
    }

    // ==================== Notification helpers ====================

    /** TRUE nếu app đã có quyền + đang bật thông báo */
    private fun isNotificationEnabled(): Boolean {
        val ctx = requireContext()
        // 1) Hệ thống đang cho phép thông báo cho app?
        val enabledInSystem = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        if (!enabledInSystem) return false

        // 2) Trên Android 13+ cần thêm quyền POST_NOTIFICATIONS
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    PermissionChecker.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Ẩn/hiện thẻ cảnh báo theo trạng thái hiện tại */
    private fun updateWarnCardVisibility() {
        val card = view?.findViewById<View>(R.id.warnCard) ?: return
        card.visibility = if (isNotificationEnabled()) View.GONE else View.VISIBLE
    }

    /** Click “Cho phép nhận thông báo” */
    private fun handleAllowNotificationClick() {
        if (Build.VERSION.SDK_INT >= 33) {
            // Nếu chưa có quyền POST_NOTIFICATIONS thì xin quyền trước
            val hasRuntimePerm = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED

            if (!hasRuntimePerm) {
                requestPostNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Quyền runtime OK rồi mà vẫn chưa enable -> mở trang cài đặt thông báo của app
        openAppNotificationSettings()
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        startActivity(intent)
    }
}
