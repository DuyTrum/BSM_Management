package com.example.bsm_management.ui.dashboard


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.ItemActionBinding


class ActionAdapter(private val onClick: (ActionItem) -> Unit) :
    ListAdapter<ActionItem, ActionAdapter.VH>(DIFF) {


    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ActionItem>() {
            override fun areItemsTheSame(oldItem: ActionItem, newItem: ActionItem) = oldItem.title == newItem.title
            override fun areContentsTheSame(oldItem: ActionItem, newItem: ActionItem) = oldItem == newItem
        }
    }


    inner class VH(val vb: ItemActionBinding) : RecyclerView.ViewHolder(vb.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }


    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.vb) {
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            if (item.subtitle.isNullOrBlank()) tvSubtitle.visibility = View.GONE else {
                tvSubtitle.visibility = View.VISIBLE; tvSubtitle.text = item.subtitle
            }
            if (item.badge != null && item.badge > 0) {
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = item.badge.toString()
            } else tvBadge.visibility = View.GONE
            root.setOnClickListener { onClick(item) }
        }
    }
}