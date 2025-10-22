package com.example.bsm_management.ui.contract

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class ContractListAdapter(
    private val items: List<ContractListItem>,
    private val onItemClick: (ContractListItem) -> Unit
) : RecyclerView.Adapter<ContractListAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvMainStatus: TextView = v.findViewById(R.id.tvMainStatus)
        val tvRent: TextView = v.findViewById(R.id.tvRent)
        val tvDeposit: TextView = v.findViewById(R.id.tvDeposit)
        val tvCreatedDate: TextView = v.findViewById(R.id.tvCreatedDate)
        val tvEndDate: TextView = v.findViewById(R.id.tvEndDate)
        val btnMore: ImageView = v.findViewById(R.id.btnMore)

        init {
            v.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
            btnMore.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contract_list, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = items[pos]

        h.tvTitle.text = c.roomName
        h.tvMainStatus.text = c.status
        h.tvRent.text = c.rent
        h.tvDeposit.text = c.deposit
        h.tvCreatedDate.text = c.createdDate
        h.tvEndDate.text = c.endDate
    }


    override fun getItemCount() = items.size
}
