package com.example.bsm_management.ui.contract

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class RoomCardListAdapter(
    private val onClick: (ContractRoomItem) -> Unit
) : ListAdapter<ContractRoomItem, RoomCardListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ContractRoomItem>() {
            override fun areItemsTheSame(o: ContractRoomItem, n: ContractRoomItem) = o.roomName == n.roomName
            override fun areContentsTheSame(o: ContractRoomItem, n: ContractRoomItem) = o == n
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v.findViewById(R.id.cardRoom)
        val tvRoomName: TextView = v.findViewById(R.id.tvRoomName)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val chipEmpty: TextView = v.findViewById(R.id.chipEmpty)
        val chipNextCycle: TextView = v.findViewById(R.id.chipNextCycle)
        val btnChevron: ImageView = v.findViewById(R.id.btnChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // item_room_card có <merge/> nên inflate như bình thường với parent, attachToRoot=false
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = getItem(pos)
        h.tvRoomName.text = it.roomName
        h.tvPrice.text = it.price

        h.chipEmpty.visibility = if (it.statusEmpty) View.VISIBLE else View.GONE
        h.chipNextCycle.visibility = if (it.statusNextCycle) View.VISIBLE else View.GONE

        // click toàn card
        h.root.setOnClickListener { onClick(it) }
        // hoặc chỉ chevron
        h.btnChevron.setOnClickListener { onClick(it) }
    }
}
