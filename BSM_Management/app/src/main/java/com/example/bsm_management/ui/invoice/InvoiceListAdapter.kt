package com.example.bsm_management.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceListAdapter(
    private val onItemClick: (InvoiceCardItem) -> Unit = {},
    private val onMoreClick: (InvoiceCardItem, View) -> Unit = { _, _ -> }
) : ListAdapter<InvoiceCardItem, InvoiceListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InvoiceCardItem>() {
            override fun areItemsTheSame(o: InvoiceCardItem, n: InvoiceCardItem) = o.id == n.id
            override fun areContentsTheSame(o: InvoiceCardItem, n: InvoiceCardItem) = o == n
        }

        private val dfIn  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val dfMon = SimpleDateFormat("'T.'M", Locale.getDefault()) // ví dụ: T.9
        private val dfY   = SimpleDateFormat("yyyy", Locale.getDefault())
        private val vnNF  = NumberFormat.getInstance(Locale("vi", "VN"))
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvMonthShort  = v.findViewById<TextView>(R.id.tvMonthShort)
        private val tvYear        = v.findViewById<TextView>(R.id.tvYear)
        private val tvTitle       = v.findViewById<TextView>(R.id.tvTitle)
        private val tvSub         = v.findViewById<TextView>(R.id.tvSub)
        private val tvNote        = v.findViewById<TextView>(R.id.tvNote)
        private val btnDetail     = v.findViewById<TextView>(R.id.btnDetail)

        // 3 cột ngày mới
        private val tvCreatedDate = v.findViewById<TextView>(R.id.tvCreatedDate)
        private val tvMoveInDate  = v.findViewById<TextView>(R.id.tvMoveInDate)
        private val tvEndDate     = v.findViewById<TextView>(R.id.tvEndDate)

        // Tổng/Paid/Remain
        private val tvTotal       = v.findViewById<TextView>(R.id.tvTotal)
        private val tvPaid        = v.findViewById<TextView>(R.id.tvPaid)
        private val tvRemain      = v.findViewById<TextView>(R.id.tvRemain)

        fun bind(item: InvoiceCardItem) {
            // Header
            tvTitle.text = item.title
            tvSub.text   = item.mainStatus

            // tvNote: dùng làm cảnh báo/ghi chú nếu có (để trống thì ẩn)
            if (tvNote.text.isNullOrBlank()) tvNote.visibility = View.GONE else tvNote.visibility = View.VISIBLE

            // Badge tháng/năm từ createdDate
            runCatching { dfIn.parse(item.createdDate) }.onSuccess { d ->
                if (d != null) {
                    tvMonthShort.text = dfMon.format(d)
                    tvYear.text = dfY.format(d)
                } else fallbackMonthYear(item)
            }.onFailure { fallbackMonthYear(item) }

            // 3 cột ngày
            tvCreatedDate.text = item.createdDate
            tvMoveInDate.text  = item.moveInDate
            tvEndDate.text     = item.endDate

            // Tổng/Paid/Còn lại
            tvTotal.text = item.rent
            tvPaid.text  = item.collected

            val rentVnd = parseMoneyVnd(item.rent)           // ví dụ: "3.000.000đ" -> 3000000
            val paidVnd = extractPaidVnd(item.collected)     // "Đã thu 3.500.000đ" -> 3500000; "Chưa thu" -> 0
            val remain  = (rentVnd - paidVnd).coerceAtLeast(0)

            tvRemain.text = "${vnNF.format(remain)} đ"

            // Clicks
            itemView.setOnClickListener { onItemClick(item) }
            btnDetail.setOnClickListener { onMoreClick(item, it) }
        }

        private fun fallbackMonthYear(item: InvoiceCardItem) {
            val parts = item.createdDate.split("/")
            tvMonthShort.text = if (parts.size == 3) "T.${parts[1]}" else "T.?"
            tvYear.text = if (parts.size == 3) parts[2] else "----"
        }

        private fun parseMoneyVnd(text: String): Long {
            // Lấy tất cả chữ số trong chuỗi, ví dụ "3.000.000đ" -> "3000000"
            val digits = text.filter { it.isDigit() }
            return digits.toLongOrNull() ?: 0L
        }

        private fun extractPaidVnd(collected: String): Long {
            // Hỗ trợ các chuỗi như "Đã thu 3.000.000đ", "Chưa thu", "Đã thu"
            val hasPaid = collected.trim().lowercase(Locale.getDefault()).startsWith("đã thu")
            return if (!hasPaid) 0L else parseMoneyVnd(collected)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
