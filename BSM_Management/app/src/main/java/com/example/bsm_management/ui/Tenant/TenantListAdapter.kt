package com.example.bsm_management.ui.tenant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemTenantBinding

class TenantListAdapter(
    private val onCall: (String) -> Unit,
    private val onMore: (Tenant) -> Unit
) : ListAdapter<Tenant, TenantListAdapter.VH>(Diff()) {

    inner class VH(val vb: ItemTenantBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemTenantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)

        with(holder.vb) {
            tvName.text = t.name
            tvPhone.text = "SĐT: ${t.phone}"

            btnCall.setOnClickListener { onCall(t.phone) }
            btnMore.setOnClickListener { onMore(t) }

            layoutNotUseApp.visibility = if (!t.isUsingApp) View.VISIBLE else View.GONE
            tagTempResidence.visibility = if (!t.hasTemporaryResidence) View.VISIBLE else View.GONE
            tagDocuments.visibility = if (!t.hasEnoughDocuments) View.VISIBLE else View.GONE
        }
    }

    class Diff : DiffUtil.ItemCallback<Tenant>() {
        override fun areItemsTheSame(o: Tenant, n: Tenant) = o.id == n.id
        override fun areContentsTheSame(o: Tenant, n: Tenant) = o == n
    }
}
