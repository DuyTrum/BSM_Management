package com.example.bsm_management.ui.tenant

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.*
import kotlin.random.Random
import com.example.bsm_management.R

class TenantDetailDialog(
    private val context: Context,
    private val tenant: Tenant,
    private val onSaved: (Tenant) -> Unit      // callback update
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_tenant_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(0x00000000))

        val tvName = dialog.findViewById<TextView>(R.id.tvName)
        val tvPhone = dialog.findViewById<TextView>(R.id.tvPhone)

        val etName = dialog.findViewById<EditText>(R.id.etName)
        val etPhone = dialog.findViewById<EditText>(R.id.etPhone)

        val layoutView = dialog.findViewById<View>(R.id.layoutViewMode)
        val layoutEdit = dialog.findViewById<View>(R.id.layoutEditMode)

        val btnEdit = dialog.findViewById<Button>(R.id.btnEdit)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnClose = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        // Load data xem chi tiết
        tvName.text = "Tên: ${tenant.name}"
        tvPhone.text = "SĐT: ${tenant.phone}"

        // Nhấn nút chỉnh sửa
        btnEdit.setOnClickListener {
            layoutView.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE

            etName.setText(tenant.name)
            etPhone.setText(tenant.phone)

            btnEdit.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
        }

        // Lưu chỉnh sửa
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updated = tenant.copy(
                name = newName,
                phone = newPhone
            )

            onSaved(updated)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
