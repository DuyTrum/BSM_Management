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
    private val onMoreClick: (view: View, item: InvoiceCardItem) -> Unit
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
        // Header/tiêu đề
        private val tvTitle: TextView      = view.findViewById(R.id.tvTitle)
        private val tvSub: TextView        = view.findViewById(R.id.tvSub)       // dùng làm "mainStatus"
        private val tvNote: TextView       = view.findViewById(R.id.tvNote)      // có thể để trống nếu chưa dùng
        private val btnDetail: TextView    = view.findViewById(R.id.btnDetail)

        // Ba cột ngày
        private val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)
        private val tvMoveInDate: TextView  = view.findViewById(R.id.tvMoveInDate)
        private val tvEndDate: TextView     = view.findViewById(R.id.tvEndDate)

        // Khu tổng tiền
        private val tvTotal: TextView   = view.findViewById(R.id.tvTotal)   // map từ item.rent (tổng hiển thị)
        private val tvPaid: TextView    = view.findViewById(R.id.tvPaid)    // map từ item.collected (Đã thu/Chưa thu)
        private val tvRemain: TextView  = view.findViewById(R.id.tvRemain)  // map từ item.deposit (đang dùng để hiển thị số còn lại)

        fun bind(item: InvoiceCardItem) {
            // Tiêu đề + trạng thái
            tvTitle.text = item.title
            tvSub.text = item.mainStatus
            // tvNote: để nguyên hoặc set theo nhu cầu
            // tvNote.text = "..."  // nếu có ghi chú riêng cho item

            // Ngày/thời điểm
            tvCreatedDate.text = item.createdDate
            tvMoveInDate.text = item.moveInDate
            tvEndDate.text = item.endDate

            // Số tiền
            tvTotal.text = item.rent        // tổng tiền hiển thị (đang truyền dạng "1.234.000đ")
            tvPaid.text = item.collected    // "Đã thu xxxđ" hoặc "Chưa thu"
            tvRemain.text = item.deposit    // đang dùng trường 'deposit' để hiển thị "Còn lại"

            // Clicks
            itemView.setOnClickListener { onItemClick(item) }
            btnDetail.setOnClickListener { v -> onMoreClick(v, item) }
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
