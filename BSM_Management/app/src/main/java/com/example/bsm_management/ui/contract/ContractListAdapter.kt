package com.example.bsm_management.ui.contract

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class ContractAdapter(private val items: List<Contract>) :
    RecyclerView.Adapter<ContractAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvMainStatus: TextView = v.findViewById(R.id.tvMainStatus)
        val tvRent: TextView = v.findViewById(R.id.tvRent)
        val tvDeposit: TextView = v.findViewById(R.id.tvDeposit)
        val tvCollected: TextView = v.findViewById(R.id.tvCollected)
        val tvCreatedDate: TextView = v.findViewById(R.id.tvCreatedDate)
        val tvMoveInDate: TextView = v.findViewById(R.id.tvMoveInDate)
        val tvEndDate: TextView = v.findViewById(R.id.tvEndDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contract_list, parent, false)   // layout mà bạn gửi
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = items[pos]
        h.tvTitle.text = "${c.roomName} - #${c.contractCode}"
        h.tvMainStatus.text = c.status
        h.tvRent.text = c.rent
        h.tvDeposit.text = c.deposit
        h.tvCollected.text = c.collected
        h.tvCreatedDate.text = c.createdDate
        h.tvMoveInDate.text = c.moveInDate
        h.tvEndDate.text = c.endDate
    }

    override fun getItemCount() = items.size
}
