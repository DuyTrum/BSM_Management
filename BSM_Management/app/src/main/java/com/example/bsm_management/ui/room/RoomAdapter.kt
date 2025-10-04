package com.example.bsm_management.ui.room

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.databinding.ItemRoomBinding
import java.text.NumberFormat
import java.util.Locale

class RoomAdapter(
    private val onPhoneClick: (String?) -> Unit,
    private val onMoreClick: (Long) -> Unit,
    private val onFillClick: (Long) -> Unit,
    private val onPostClick: (Long) -> Unit
) : ListAdapter<UiRoom, RoomAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UiRoom>() {
            override fun areItemsTheSame(o: UiRoom, n: UiRoom) = o.id == n.id
            override fun areContentsTheSame(o: UiRoom, n: UiRoom) = o == n
        }
    }

    inner class VH(val vb: ItemRoomBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(h: VH, pos: Int) = with(h.vb) {
        val room = getItem(pos)
        val ctx = h.itemView.context

        // Tên, giá thuê, số khách
        tvRoomName.text = room.name
        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
        tvRent.text = "${nf.format(room.baseRent)} đ"
        tvTenantCount.text = "${room.tenantCount}/1 người"

        // Phone
        tvPhone.apply {
            text = room.phone ?: ctx.getString(R.string.no_phone)
            val color = if (room.phone != null)
                ContextCompat.getColor(ctx, R.color.bsm_link)
            else
                ContextCompat.getColor(ctx, R.color.bsm_text_secondary)
            setTextColor(color)
            setOnClickListener { onPhoneClick(room.phone) }
        }

        // Contract rows
        rowContractDate.value.text =
            room.contractEnd ?: ctx.getString(R.string.contract_empty)

        rowApp.value.text = ctx.getString(
            if (room.appUsed) R.string.app_used else R.string.app_not_used
        )
        rowApp.value.setTextColor(
            ContextCompat.getColor(
                ctx, if (room.appUsed) R.color.bsm_success else R.color.bsm_orange
            )
        )

        rowOnline.value.text = ctx.getString(
            if (room.onlineSigned) R.string.online_signed else R.string.online_not_signed
        )
        rowOnline.value.setTextColor(
            ContextCompat.getColor(
                ctx, if (room.onlineSigned) R.color.bsm_success else R.color.bsm_orange
            )
        )

        // Status chips
        chipOccupied.isVisible = room.status == "OCCUPIED"
        chipWaiting.isVisible  = true // luôn hiển thị “Chờ kỳ thu tới”
        chipEmpty.isVisible    = room.status != "OCCUPIED"

        // Click 3-chấm & actions
        btnMore.setOnClickListener { onMoreClick(room.id) }
        btnFill.setOnClickListener { onFillClick(room.id) }
        btnPost.setOnClickListener { onPostClick(room.id) }
    }
}
