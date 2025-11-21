package com.example.bsm_management.ui.tenant

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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

        val warn = v.findViewById<TextView>(R.id.tvWarning)
        warn.visibility = if (!tenant.isUsingApp) View.VISIBLE else View.GONE

        // ========== 1. CHAT ==========
        v.findViewById<View>(R.id.rowChat).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${tenant.phone}"))
            startActivity(intent)
            dismiss()
        }

        // ========== 2. CHO PHÉP DÙNG APP ==========
        v.findViewById<View>(R.id.rowAllowApp).setOnClickListener {
            tenant.isUsingApp = true

            val db = DatabaseHelper(safeContext)
            db.updateTenantUsingApp(tenant.id, true)

            onTenantUpdated(tenant)
            Toast.makeText(safeContext, "Đã cho phép khách dùng APP", Toast.LENGTH_SHORT).show()

            dismiss()
        }

        // ========== 3. XEM / SỬA (DIALOG DETAIL) ==========
        v.findViewById<View>(R.id.rowDetail).setOnClickListener {
            showDetailDialog()
            dismiss()
        }

        // ========== 4. CHỈNH SỬA ==========
        v.findViewById<View>(R.id.rowEdit).setOnClickListener {
            showDetailDialog()
            dismiss()
        }

        // ========== 5. IN TỜ KHAI ==========
        v.findViewById<View>(R.id.rowPrint).setOnClickListener {
            Toast.makeText(safeContext, "Đang tạo PDF...", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        // ========== 6. XÓA ==========
        v.findViewById<View>(R.id.rowDelete).setOnClickListener {
            confirmDelete()
        }

        return v
    }


    // ======================
    //  HÀM HỖ TRỢ AN TOÀN
    // ======================

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
            .setMessage("Bạn có chắc muốn xóa '${tenant.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                val db = DatabaseHelper(safeContext)
                db.moveTenantToOld(tenant.id)
                onTenantDeleted(tenant.id)
                Toast.makeText(safeContext, "Đã xóa khách thuê", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
