package com.example.bsm_management.ui.tenant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class TenantSlotAdapter(
    private val maxPeople: Int,
    private var tenants: List<Tenant?>,
    private val onAddTenant: (slotIndex: Int) -> Unit,
    private val onEditTenant: (Tenant) -> Unit,
    private val onDeleteTenant: (Tenant) -> Unit
) : RecyclerView.Adapter<TenantSlotAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cardEmpty: LinearLayout = v.findViewById(R.id.cardEmpty)
        val cardTenant: LinearLayout = v.findViewById(R.id.cardTenant)

        val tvTenantName: TextView = v.findViewById(R.id.tvTenantName)
        val tvTenantPhone: TextView = v.findViewById(R.id.tvTenantPhone)

        val btnEdit: ImageButton = v.findViewById(R.id.btnTenantEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnTenantDelete)
        val tvAdd: TextView = v.findViewById(R.id.tvAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tenant_slot, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = maxPeople

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tenant = tenants.getOrNull(position)

        if (tenant == null) {
            // ===============================
            //    SLOT TRỐNG
            // ===============================
            holder.cardEmpty.visibility = View.VISIBLE
            holder.cardTenant.visibility = View.GONE

            holder.cardEmpty.setOnClickListener {
                onAddTenant(position + 1) // slotIndex tính từ 1
            }
        } else {
            // ===============================
            //    SLOT CÓ KHÁCH
            // ===============================
            holder.cardEmpty.visibility = View.GONE
            holder.cardTenant.visibility = View.VISIBLE

            holder.tvTenantName.text = tenant.name
            holder.tvTenantPhone.text = tenant.phone

            holder.btnEdit.setOnClickListener { onEditTenant(tenant) }
            holder.btnDelete.setOnClickListener { onDeleteTenant(tenant) }
        }
    }

    fun updateTenants(newList: List<Tenant?>) {
        tenants = newList
        notifyDataSetChanged()
    }
}
