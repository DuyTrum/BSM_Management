package com.example.bsm_management.ui.message

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemInboxBinding

class InboxAdapter : RecyclerView.Adapter<InboxAdapter.VH>() {

    private val items = mutableListOf<InboxItem>()

    fun removeAt(pos: Int) {
        if (pos in items.indices) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    /** Nạp danh sách mới */
    fun submitList(list: List<InboxItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** ✅ Hàm dùng cho Swipe-to-delete */
    fun getCurrentItems(): List<InboxItem> = items

    /** ViewHolder */
    inner class VH(val vb: ItemInboxBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemInboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) = with(holder.vb) {
        val msg = items[pos]
        tvHostel.text = msg.hostelName
        tvSender.text = msg.sender
        tvMessage.text = msg.message
        tvTime.text = msg.time

        // nếu chưa đọc => in đậm
        val style = if (msg.isRead) Typeface.NORMAL else Typeface.BOLD
        tvMessage.setTypeface(null, style)
        tvSender.setTypeface(null, style)
    }
}
