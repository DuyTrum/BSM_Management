package com.example.bsm_management.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemMessageBinding

class MessageAdapter(private val onClick: (MessageItem) -> Unit) :
    ListAdapter<MessageItem, MessageAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(o: MessageItem, n: MessageItem) = o.title == n.title && o.time == n.time
            override fun areContentsTheSame(o: MessageItem, n: MessageItem) = o == n
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
            tvTitle.text = item.title
            tvSubtitle.text = item.content
            tvTime.text = item.time
            ivPin.visibility = if (item.pinned) View.VISIBLE else View.GONE
            root.setOnClickListener { onClick(item) }
        }
    }
}

