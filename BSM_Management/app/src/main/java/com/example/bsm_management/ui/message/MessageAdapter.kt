package com.example.bsm_management.ui.message

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemInboxBinding
import database.DatabaseHelper  // <-- Quan trọng

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.VH>() {

    // ✅ Danh sách tin nhắn đúng kiểu dữ liệu
    private val items = mutableListOf<DatabaseHelper.Message>()

    /** Xóa item tại vị trí */
    fun removeAt(pos: Int) {
        if (pos in items.indices) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    /** Nạp danh sách mới từ SQLite */
    fun submitList(list: List<DatabaseHelper.Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** Dùng cho Swipe-to-delete */
    fun getCurrentItems(): List<DatabaseHelper.Message> = items

    /** ViewHolder */
    inner class VH(val vb: ItemInboxBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemInboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) = with(holder.vb) {
        val msg = items[pos]

        // Nếu layout có tvHostel, gán tên trọ nếu có
        tvHostel?.text = msg.hostelName ?: ""

        tvSender.text = msg.sender
        tvMessage.text = msg.message
        tvTime.text = msg.time

        // In đậm nếu tin chưa đọc
        val style = if (msg.isRead) Typeface.NORMAL else Typeface.BOLD
        tvMessage.setTypeface(null, style)
        tvSender.setTypeface(null, style)
    }
}
