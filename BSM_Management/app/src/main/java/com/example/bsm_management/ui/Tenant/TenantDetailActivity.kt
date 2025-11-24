package com.example.bsm_management.ui.tenant

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)


        val tvName = dialog.findViewById<TextView>(R.id.tvName)
        val tvPhone = dialog.findViewById<TextView>(R.id.tvPhone)
        val tvCccd = dialog.findViewById<TextView>(R.id.tvCccd)
        val tvAddress = dialog.findViewById<TextView>(R.id.tvAddress)


        val etName = dialog.findViewById<EditText>(R.id.etName)
        val etPhone = dialog.findViewById<EditText>(R.id.etPhone)
        val etCccd = dialog.findViewById<EditText>(R.id.etCccd)
        val etAddress = dialog.findViewById<EditText>(R.id.etAddress)


        val layoutView = dialog.findViewById<View>(R.id.layoutViewMode)
        val layoutEdit = dialog.findViewById<View>(R.id.layoutEditMode)

        val btnEdit = dialog.findViewById<Button>(R.id.btnEdit)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnClose = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        // Load data xem chi tiết
        tvName.text = "Tên: ${tenant.name}"
        tvPhone.text = "SĐT: ${tenant.phone}"
        tvCccd.text = "CCCD: ${tenant.cccd ?: "---"}"
        tvAddress.text = "Địa chỉ: ${tenant.address ?: "---"}"


        // Nhấn nút chỉnh sửa
        btnEdit.setOnClickListener {
            layoutView.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE

            etName.setText(tenant.name)
            etPhone.setText(tenant.phone)
            etCccd.setText(tenant.cccd)
            etAddress.setText(tenant.address)

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
                phone = newPhone,
                cccd = etCccd.text.toString().trim(),
                address = etAddress.text.toString().trim()
            )


            onSaved(updated)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
