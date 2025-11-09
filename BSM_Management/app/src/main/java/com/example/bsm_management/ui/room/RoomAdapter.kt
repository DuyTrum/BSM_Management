package com.example.bsm_management.ui.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class RoomAdapter(
    private val onPhoneClick: (String?) -> Unit,
    private val onMoreClick: (Long) -> Unit,
    private val onFillClick: (Long) -> Unit,
    private val onPostClick: (Long) -> Unit
) : ListAdapter<UiRoom, RoomAdapter.RoomVH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomVH(v)
    }

    override fun onBindViewHolder(holder: RoomVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomVH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvRoomName: TextView = v.findViewById(R.id.tvRoomName)
        private val tvPhone: TextView = v.findViewById(R.id.tvPhone)
        private val tvRent: TextView = v.findViewById(R.id.tvRent)
        private val tvTenantCount: TextView = v.findViewById(R.id.tvTenantCount)
        private val chipOccupied: TextView = v.findViewById(R.id.chipOccupied)
        private val chipEmpty: TextView = v.findViewById(R.id.chipEmpty)
        private val btnFill: com.google.android.material.button.MaterialButton = v.findViewById(R.id.btnFill)
        private val btnPost: com.google.android.material.button.MaterialButton = v.findViewById(R.id.btnPost)
        private val btnMore: ImageButton = v.findViewById(R.id.btnMore)

        fun bind(item: UiRoom) {
            val ctx = itemView.context
            tvRoomName.text = item.name.ifBlank { "Phòng chưa đặt tên" }
            tvPhone.text = item.phone ?: "Chưa có số điện thoại"
            tvRent.text = if (item.baseRent != null && item.baseRent > 0)
                "${item.baseRent} đ/tháng" else "Chưa có giá"
            tvTenantCount.text = "${item.tenantCount ?: 0} người"

            chipOccupied.isVisible = item.status == "OCCUPIED"
            chipEmpty.isVisible = item.status == "EMPTY"

            // Nút lấp phòng / trả phòng toggle
            btnFill.apply {
                if (item.status == "OCCUPIED") {
                    text = "Trả phòng"
                    setIconResource(R.drawable.ic_logout)
                    setTextColor(ContextCompat.getColor(ctx, R.color.bsm_orange))
                    iconTint = ContextCompat.getColorStateList(ctx, R.color.bsm_orange)
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.bsm_orange)
                } else {
                    text = "Lấp phòng"
                    setIconResource(R.drawable.ic_bolt)
                    setTextColor(ContextCompat.getColor(ctx, R.color.bsm_green))
                    iconTint = ContextCompat.getColorStateList(ctx, R.color.bsm_green)
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.bsm_green)
                }
                setOnClickListener { onFillClick(item.id) }
            }

            btnPost.setOnClickListener { onPostClick(item.id) }
            btnMore.setOnClickListener { onMoreClick(item.id) }
            tvPhone.setOnClickListener { onPhoneClick(item.phone) }
        }
    }

    class Diff : DiffUtil.ItemCallback<UiRoom>() {
        override fun areItemsTheSame(oldItem: UiRoom, newItem: UiRoom) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UiRoom, newItem: UiRoom) = oldItem == newItem
    }
}
