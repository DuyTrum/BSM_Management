package com.example.bsm_management.ui.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.google.android.material.button.MaterialButton

class RoomAdapter(
    private val onPhoneClick: (String?) -> Unit,
    private val onRoomClick: (UiRoom) -> Unit,
    private val onMoreClick: (Long) -> Unit,
    private val onFillClick: (Long) -> Unit,
    private val onEditRoomPriceClick: (Long, Int?) -> Unit,
    private val onEditServicePriceClick: (Long, String, Int?) -> Unit
    ) : ListAdapter<UiRoom, RoomAdapter.RoomVH>(Diff()) {

    private var serviceList: List<Triple<String, Boolean, Int>> = emptyList()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomVH(v)
    }

    override fun onBindViewHolder(holder: RoomVH, position: Int) {
        val item = getItem(position)
        holder.bind(item, serviceList)

        holder.tvTenantCount.text =
            if (item.maxPeople == 0) "Không giới hạn"
            else "${item.maxPeople} người"
        holder.bind(item, item.services ?: emptyList())
    }

    inner class RoomVH(v: View) : RecyclerView.ViewHolder(v) {

        val tvTenantCount: TextView = v.findViewById(R.id.tvTenantCount)

        private val tvRoomName: TextView = v.findViewById(R.id.tvRoomName)
        private val tvPhone: TextView = v.findViewById(R.id.tvPhone)
        private val tvRent: TextView = v.findViewById(R.id.tvRent)
        private val chipOccupied: TextView = v.findViewById(R.id.chipOccupied)
        private val chipEmpty: TextView = v.findViewById(R.id.chipEmpty)
        private val btnFill: MaterialButton = v.findViewById(R.id.btnFill)
        private val btnMore: ImageButton = v.findViewById(R.id.btnMore)
        private val btnEditRoomPrice: ImageButton = v.findViewById(R.id.btnEditRoomPrice)
        private val layoutServices: LinearLayout = v.findViewById(R.id.layoutServices)

        fun bind(item: UiRoom, services: List<Triple<String, Boolean, Int>>) {

            tvRoomName.text = item.name
            tvPhone.text = item.phone ?: "Chưa có số điện thoại"
            tvRent.text = "${item.baseRent} đ/tháng"

            chipOccupied.isVisible = item.status == "RENTED"
            chipEmpty.isVisible = item.status == "EMPTY"

            btnEditRoomPrice.setOnClickListener {
                onEditRoomPriceClick(item.id, item.baseRent)
            }
            itemView.setOnClickListener {
                onRoomClick(item)
            }

            layoutServices.removeAllViews()
            if (item.status == "EMPTY") {
                btnFill.text = "Lắp phòng"
                btnFill.setBackgroundResource(R.drawable.bg_button_green)
            } else {
                btnFill.text = "Trả phòng"
                btnFill.setBackgroundResource(R.drawable.bg_button_gray)
            }

            val ctx = itemView.context

            for ((name, enabled, price) in services) {
                val row = LayoutInflater.from(ctx)
                    .inflate(R.layout.row_service_item, layoutServices, false)

                val icon = row.findViewById<ImageView>(R.id.ivServiceIcon)
                val tvName = row.findViewById<TextView>(R.id.tvServiceName)
                val tvPrice = row.findViewById<TextView>(R.id.tvServicePrice)
                val btnEdit = row.findViewById<ImageButton>(R.id.btnEditService)

                tvName.text = name
                tvPrice.text = if (price > 0) "$price đ" else "Chưa đặt giá"

                icon.setImageResource(
                    when {
                        name.contains("điện", true) -> R.drawable.ic_bolt
                        name.contains("nước", true) -> R.drawable.ic_water_drop
                        name.contains("rác", true) -> R.drawable.ic_delete_outline
                        name.contains("internet", true) -> R.drawable.ic_wifi
                        else -> R.drawable.ic_settings
                    }
                )

                row.alpha = if (enabled) 1f else 0.4f
                btnEdit.setOnClickListener {
                    onEditServicePriceClick(
                        item.id,   // roomId
                        name,      // serviceName
                        price      // currentPrice
                    )
                }


                layoutServices.addView(row)
            }

            tvPhone.setOnClickListener { onPhoneClick(item.phone) }
            btnFill.setOnClickListener { onFillClick(item.id) }
            btnMore.setOnClickListener { onMoreClick(item.id) }
        }
    }

    class Diff : DiffUtil.ItemCallback<UiRoom>() {
        override fun areItemsTheSame(old: UiRoom, new: UiRoom) = old.id == new.id
        override fun areContentsTheSame(old: UiRoom, new: UiRoom) = old == new
    }
}
