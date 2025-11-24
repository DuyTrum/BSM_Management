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
import com.example.bsm_management.ui.room.RoomListActivity
import com.example.bsm_management.ui.tenant.TenantManagerActivity
import com.google.android.material.button.MaterialButton

class DashboardManageFragment : Fragment(R.layout.fragment_dashboard_manage) {

    // Notification launcher
    private val requestPostNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateWarnCardVisibility()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CARD CẢNH BÁO THÔNG BÁO
        view.findViewById<MaterialButton>(R.id.btnAllowNotification).setOnClickListener {
            handleAllowNotificationClick()
        }
        updateWarnCardVisibility()

        // ========== GRID MENU (PHÒNG + KHÁCH THUÊ) ==========

        val rvMenu = view.findViewById<RecyclerView>(R.id.rvMenu)
        val span = 2

        val menuAdapter = ActionAdapter(
            onClick = { item ->
                when (item.title) {
                    "Quản lý phòng" -> startActivity(Intent(requireContext(), RoomListActivity::class.java))
                    "Lập hợp đồng" ->
                        startActivity(Intent(requireContext(), ContractRoomActivity::class.java))

                    "Lập hóa đơn" ->
                        startActivity(Intent(requireContext(), InvoiceActivity::class.java))

                    "Quản lý hợp đồng" ->
                        startActivity(Intent(requireContext(), ContractListActivity::class.java))

                    "Quản lý hóa đơn" ->
                        startActivity(Intent(requireContext(), InvoiceListActivity::class.java))
                }
            },
            onTenantClick = {
                startActivity(Intent(requireContext(), TenantManagerActivity::class.java))
            }
        )

        rvMenu.layoutManager = GridLayoutManager(requireContext(), span)
        rvMenu.addItemDecoration(GridSpacingItemDecoration(span, 16, true))
        rvMenu.adapter = menuAdapter

        menuAdapter.submitList(
            listOf(
                // HỢP ĐỒNG – HÓA ĐƠN (TỪ THAO TÁC THƯỜNG DÙNG)
                ActionItem(R.drawable.ic_handshake, "Lập hợp đồng"),
                ActionItem(R.drawable.ic_receipt_long, "Lập hóa đơn"),
                ActionItem(R.drawable.ic_contract_manage, "Quản lý hợp đồng"),
                ActionItem(R.drawable.ic_bill_due, "Quản lý hóa đơn"),
                // PHÒNG – KHÁCH
                ActionItem(R.drawable.ic_room_manage, "Quản lý phòng"),
                ActionItem(R.drawable.ic_tenant_manage, "Quản lý khách thuê"),


            )
        )

    }

    override fun onResume() {
        super.onResume()
        autoExpireContracts()
        updateWarnCardVisibility()
    }


    // ==================== Notification Helpers ====================

    private fun isNotificationEnabled(): Boolean {
        val ctx = requireContext()

        val enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        if (!enabled) return false

        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED
        } else true
    }

    private fun updateWarnCardVisibility() {
        val card = view?.findViewById<View>(R.id.warnCard) ?: return
        card.visibility = if (isNotificationEnabled()) View.GONE else View.VISIBLE
    }

    private fun handleAllowNotificationClick() {
        if (Build.VERSION.SDK_INT >= 33) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED

            if (!hasPermission) {
                requestPostNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        openAppNotificationSettings()
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        startActivity(intent)
    }

    private fun autoExpireContracts() {
        val db = database.DatabaseHelper(requireContext()).writableDatabase
        val now = System.currentTimeMillis()

        // Chỉ set active = 0 cho hợp đồng đã hết hạn nhưng vẫn = 1
        val sql = """
        UPDATE contracts
        SET active = 0
        WHERE active = 1
        AND endDate IS NOT NULL
        AND endDate < $now
    """

        db.execSQL(sql)
    }
}
