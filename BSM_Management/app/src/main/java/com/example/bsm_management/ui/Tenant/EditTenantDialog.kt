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
    private val onSave: (String, String) -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_tenant)

        // Nền trong suốt giống AddTenantDialog
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etName = dialog.findViewById<EditText>(R.id.etName)
        val etPhone = dialog.findViewById<EditText>(R.id.etPhone)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Đổ dữ liệu cũ
        etName.setText(tenant.name)
        etPhone.setText(tenant.phone)

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            if (newName.isNotEmpty() && newPhone.isNotEmpty()) {
                onSave(newName, newPhone)
                dialog.dismiss()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Set width giống AddTenantDialog
        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
