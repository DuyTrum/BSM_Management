package com.example.bsm_management.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class InvoiceListAdapter(
    private val onItemClick: (InvoiceCardItem) -> Unit,
    private val onMoreClick: (view: View, item: InvoiceCardItem) -> Unit,
    private val onCall: (String) -> Unit
) : ListAdapter<InvoiceCardItem, InvoiceListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InvoiceCardItem>() {
            override fun areItemsTheSame(oldItem: InvoiceCardItem, newItem: InvoiceCardItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: InvoiceCardItem, newItem: InvoiceCardItem) =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView      = view.findViewById(R.id.tvTitle)
        private val tvSub: TextView        = view.findViewById(R.id.tvSub)
        private val btnDetail: TextView    = view.findViewById(R.id.btnDetail)
        private val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)
        private val tvMoveInDate: TextView  = view.findViewById(R.id.tvMoveInDate)
        private val tvEndDate: TextView     = view.findViewById(R.id.tvEndDate)
        private val btnCall: TextView = view.findViewById(R.id.btnCall)
        private val tvTotal: TextView   = view.findViewById(R.id.tvTotal)
        private val tvPaid: TextView    = view.findViewById(R.id.tvPaid)
        private val tvRemain: TextView  = view.findViewById(R.id.tvRemain)

        fun bind(item: InvoiceCardItem) {
            tvTitle.text = item.title
            tvSub.text = item.mainStatus

            tvCreatedDate.text = item.createdDate
            tvMoveInDate.text = item.moveInDate
            tvEndDate.text = item.endDate

            tvTotal.text = item.rent
            tvPaid.text = item.collected
            tvRemain.text = item.deposit

            itemView.setOnClickListener { onItemClick(item) }
            btnDetail.setOnClickListener { v -> onMoreClick(v, item) }
            btnCall.setOnClickListener  { onCall(item.phone) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
