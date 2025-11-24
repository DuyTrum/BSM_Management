package com.example.bsm_management.ui.tenant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R


class TenantListAdapter(
    val onCall: (String) -> Unit,
    val onMore: (Tenant) -> Unit
) : ListAdapter<TenantManagerActivity.TenantRow, RecyclerView.ViewHolder>(Diff()) {

    companion object {

        private const val TYPE_ROOM = 0
        private const val TYPE_TENANT = 1

        class Diff : DiffUtil.ItemCallback<TenantManagerActivity.TenantRow>() {

            override fun areItemsTheSame(
                oldItem: TenantManagerActivity.TenantRow,
                newItem: TenantManagerActivity.TenantRow
            ): Boolean {
                return when {
                    oldItem is TenantManagerActivity.TenantRow.RoomHeader &&
                            newItem is TenantManagerActivity.TenantRow.RoomHeader ->
                        oldItem.roomName == newItem.roomName

                    oldItem is TenantManagerActivity.TenantRow.TenantItem &&
                            newItem is TenantManagerActivity.TenantRow.TenantItem ->
                        oldItem.tenant.id == newItem.tenant.id

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: TenantManagerActivity.TenantRow,
                newItem: TenantManagerActivity.TenantRow
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TenantManagerActivity.TenantRow.RoomHeader -> TYPE_ROOM
            is TenantManagerActivity.TenantRow.TenantItem -> TYPE_TENANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ROOM) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_room_header, parent, false)
            RoomVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tenant, parent, false)
            TenantVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is TenantManagerActivity.TenantRow.RoomHeader -> (holder as RoomVH).bind(row)
            is TenantManagerActivity.TenantRow.TenantItem -> (holder as TenantVH).bind(row.tenant)
        }
    }

    // === ViewHolder for Room Header ===
    class RoomVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(data: TenantManagerActivity.TenantRow.RoomHeader) {
            itemView.findViewById<TextView>(R.id.tvRoomName).text = data.roomName
        }
    }

    // === ViewHolder for Tenant ===
    inner class TenantVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(t: Tenant) {
            itemView.findViewById<TextView>(R.id.tvName).text = t.name
            itemView.findViewById<TextView>(R.id.tvPhone).text = "SĐT: ${t.phone}"
            val hasAddress = !t.address.isNullOrBlank()
            itemView.findViewById<View>(R.id.tagTempResidence).visibility =
                if (hasAddress) View.GONE else View.VISIBLE

            val hasCccd = !t.cccd.isNullOrBlank()
            itemView.findViewById<View>(R.id.tagDocuments).visibility =
                if (hasCccd) View.GONE else View.VISIBLE

            itemView.findViewById<View>(R.id.layoutNotUseApp)?.visibility = View.GONE


            itemView.findViewById<View>(R.id.btnCall).setOnClickListener {
                onCall(t.phone)
            }

            itemView.findViewById<View>(R.id.btnMore).setOnClickListener {
                onMore(t)
            }
        }
    }
}


