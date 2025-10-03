// ui/contract/ContractRoomAdapter.kt
package com.example.bsm_management.ui.contract

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class ContractRoomAdapter(
    private val items: List<ContractRoomItem>,
    private val onItemClick: (ContractRoomItem) -> Unit
) : RecyclerView.Adapter<ContractRoomAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cardRoom: View = v.findViewById(R.id.cardRoom)
        val tvRoomName: TextView = v.findViewById(R.id.tvRoomName)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val chipEmpty: TextView = v.findViewById(R.id.chipEmpty)
        val chipNextCycle: TextView = v.findViewById(R.id.chipNextCycle)
        val btnChevron: ImageView = v.findViewById(R.id.btnChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_contract, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.tvRoomName.text = item.roomName
        h.tvPrice.text = item.price
        h.chipEmpty.visibility = if (item.isEmpty) View.VISIBLE else View.GONE
        h.chipNextCycle.visibility = if (item.waitNextCycle) View.VISIBLE else View.GONE

        h.cardRoom.setOnClickListener { onItemClick(item) }
        h.btnChevron.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}


