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
    private val onClickDetail: (InvoiceItem) -> Unit = {}
) : ListAdapter<InvoiceItem, InvoiceListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InvoiceItem>() {
            override fun areItemsTheSame(o: InvoiceItem, n: InvoiceItem) =
                o.roomTitle == n.roomTitle && o.monthShort == n.monthShort && o.year == n.year

            override fun areContentsTheSame(o: InvoiceItem, n: InvoiceItem) = o == n
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMonth: TextView = view.findViewById(R.id.tvMonthShort)
        private val tvYear: TextView = view.findViewById(R.id.tvYear)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSub: TextView = view.findViewById(R.id.tvSub)
        private val tvNote: TextView = view.findViewById(R.id.tvNote)
        private val btnDetail: TextView = view.findViewById(R.id.btnDetail)
        private val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        private val tvPaid: TextView = view.findViewById(R.id.tvPaid)
        private val tvRemain: TextView = view.findViewById(R.id.tvRemain)

        fun bind(it: InvoiceItem) {
            tvMonth.text = it.monthShort
            tvYear.text = it.year
            tvTitle.text = it.roomTitle
            tvSub.text = it.subtitle

            if (it.note.isNullOrBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = it.note
            }

            tvTotal.text = it.total
            tvPaid.text = it.paid
            tvRemain.text = it.remain

            // click vào nút "Chi tiết"
            btnDetail.setOnClickListener { onClickDetail(it) }
            // hoặc click cả item
            itemView.setOnClickListener { onClickDetail(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))
}
