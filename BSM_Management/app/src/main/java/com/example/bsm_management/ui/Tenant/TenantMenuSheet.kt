package com.example.bsm_management.ui.tenant

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import database.DatabaseHelper

class TenantMenuSheet(
    private val tenant: Tenant,
    private val onTenantUpdated: (Tenant) -> Unit = {},
    private val onTenantDeleted: (Int) -> Unit = {}
) : BottomSheetDialogFragment() {

    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sheet_tenant_menu, container, false)
// ==== STATUS: kiểm tra địa chỉ + CCCD ====
        val tvStatus = v.findViewById<TextView>(R.id.tvWarning)

        val hasAddress = tenant.address?.isNotBlank() == true
        val hasCccd = tenant.cccd?.isNotBlank() == true

        tvStatus.text = when {
            !hasAddress && !hasCccd -> "Khách chưa khai tạm trú và CCCD"
            !hasAddress -> "Khách chưa khai tạm trú"
            !hasCccd -> "Khách chưa khai CCCD"
            else -> "Khách đã khai đủ thông tin"
        }
        // 2. Xem chi tiết / chỉnh sửa
        v.findViewById<View>(R.id.rowDetail).setOnClickListener {
            showDetailDialog()
            dismiss()
        }

        // 4. Xoá
        v.findViewById<View>(R.id.rowDelete).setOnClickListener {
            confirmDelete()
        }

        return v
    }

    private fun showDetailDialog() {
        TenantDetailDialog(safeContext, tenant) { updated ->
            val db = DatabaseHelper(safeContext)
            db.updateTenant(updated)
            onTenantUpdated(updated)
        }.show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(safeContext)
            .setTitle("Xóa khách thuê")
            .setMessage("Bạn có chắc muốn xoá '${tenant.name}'?")
            .setPositiveButton("Xoá") { _, _ ->
                val db = DatabaseHelper(safeContext)
                db.moveTenantToOld(safeContext, tenant)
                onTenantDeleted(tenant.id)
                Toast.makeText(safeContext, "Đã xoá khách thuê", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}

