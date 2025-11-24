package com.example.bsm_management.ui.tenant

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.bsm_management.R

class EditTenantDialog(
    private val context: Context,
    private val tenant: Tenant,
    private val onSave: (Tenant) -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_tenant)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etName = dialog.findViewById<EditText>(R.id.etName)
        val etPhone = dialog.findViewById<EditText>(R.id.etPhone)
        val etCccd = dialog.findViewById<EditText>(R.id.etCccd)
        val etAddress = dialog.findViewById<EditText>(R.id.etAddress)

        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Đổ dữ liệu cũ
        etName.setText(tenant.name)
        etPhone.setText(tenant.phone)
        etCccd.setText(tenant.cccd ?: "")
        etAddress.setText(tenant.address ?: "")

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
            val newCccd = etCccd.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty()) {
                return@setOnClickListener
            }

            val updatedTenant = tenant.copy(
                name = newName,
                phone = newPhone,
                cccd = newCccd,
                address = newAddress
            )

            onSave(updatedTenant)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
