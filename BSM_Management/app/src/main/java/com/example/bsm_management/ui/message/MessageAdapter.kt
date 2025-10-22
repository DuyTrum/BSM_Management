package com.example.bsm_management.ui.message

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemMessageBinding
import com.example.bsm_management.ui.model.MessageItem

class MessageAdapter(
    private val onClick: (MessageItem) -> Unit
) : ListAdapter<MessageItem, MessageAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(val vb: ItemMessageBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(vb)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.vb) {
            tvTitle.text = item.title
            tvSubtitle.text = item.content.orEmpty()

            // createdAt (millis) -> "x phút trước" / "hôm qua"...
            tvTime.text = DateUtils.getRelativeTimeSpanString(
                item.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            ivPin.visibility = if (item.pinned) View.VISIBLE else View.GONE

            root.setOnClickListener { onClick(item) }
        }
    }
}
