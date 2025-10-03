package com.example.bsm_management.ui.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemMessageBinding

class MessageAdapter(private val onClick: (MessageItem) -> Unit) :
    ListAdapter<MessageItem, MessageAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(old: MessageItem, new: MessageItem) = old === new
            override fun areContentsTheSame(old: MessageItem, new: MessageItem) = old == new
        }
    }

    inner class VH(val vb: ItemMessageBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.vb) {
            tvSender.text = item.sender
            tvContent.text = item.content
            tvTime.text = item.time
            root.alpha = if (item.unread) 1f else 0.6f
            root.setOnClickListener { onClick(item) }
        }
    }
}
