package com.example.bsm_management.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class InvoiceAdapter(
    private val onItemClick: (RoomItem) -> Unit
) : ListAdapter<RoomItem, InvoiceAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RoomItem>() {
            override fun areItemsTheSame(oldItem: RoomItem, newItem: RoomItem): Boolean =
                oldItem.roomId == newItem.roomId

            override fun areContentsTheSame(oldItem: RoomItem, newItem: RoomItem): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRoomName: TextView = view.findViewById(R.id.tvRoomName)
        private val tvPhone: TextView    = view.findViewById(R.id.tvPhone)
        private val tvContract: TextView = view.findViewById(R.id.tvContract)
        private val tvRent: TextView     = view.findViewById(R.id.tvRent)
        private val tvPeople: TextView   = view.findViewById(R.id.tvPeople)
        private val tvStatus: TextView   = view.findViewById(R.id.tvStatus)

        private val tvInvoiceCount: TextView = view.findViewById(R.id.tvInvoiceCount)

        fun bind(item: RoomItem) {
            tvRoomName.text = item.roomName
            tvPhone.text    = item.phone
            tvContract.text = item.contract
            tvRent.text     = item.rent
            tvPeople.text   = item.people
            tvStatus.text   = item.status

            tvInvoiceCount.text = "${item.invoiceCount} hóa đơn tháng này"

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_room, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
