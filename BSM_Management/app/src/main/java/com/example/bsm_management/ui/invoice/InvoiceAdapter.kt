package com.example.bsm_management.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class InvoiceAdapter(
    private val onClick: (RoomItem) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.VH>() {

    private val items = mutableListOf<RoomItem>()

    fun submitList(list: List<RoomItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRoomName = view.findViewById<TextView>(R.id.tvRoomName)
        private val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        private val tvContract = view.findViewById<TextView>(R.id.tvContract)
        private val tvRent = view.findViewById<TextView>(R.id.tvRent)
        private val tvPeople = view.findViewById<TextView>(R.id.tvPeople)

        fun bind(item: RoomItem) {
            tvRoomName.text = item.roomName
            tvPhone.text = item.phone
            tvContract.text = item.contract
            tvRent.text = item.rent
            tvPeople.text = item.people

            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_room, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size
}
