package com.example.bsm_management.ui.room

import android.app.AlertDialog
import android.content.ContentValues
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.google.android.material.button.MaterialButton
import database.DatabaseHelper

class RoomAdapter(
    private val onPhoneClick: (String?) -> Unit,
    private val onMoreClick: (Long) -> Unit,
    private val onFillClick: (Long) -> Unit,
    private val onPostClick: (Long) -> Unit,
    private val onEditRoomPriceClick: (Long, Int?) -> Unit
) : ListAdapter<UiRoom, RoomAdapter.RoomVH>(Diff()) {

    private var activeServices: List<Triple<String, Boolean, Int>> = emptyList()

    fun setActiveServices(services: List<Triple<String, Boolean, Int>>) {
        activeServices = services
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
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
        private val btnFill: MaterialButton = v.findViewById(R.id.btnFill)
        private val btnPost: MaterialButton = v.findViewById(R.id.btnPost)
        private val btnMore: ImageButton = v.findViewById(R.id.btnMore)
        private val btnEditPrice: ImageButton = v.findViewById(R.id.btnEditPrice)
        private val layoutServices: LinearLayout = v.findViewById(R.id.layoutServices)

        fun bind(item: UiRoom) {
            val ctx = itemView.context
            val db = DatabaseHelper(ctx)

            tvRoomName.text = item.name.ifBlank { "Phòng chưa đặt tên" }
            tvPhone.text = item.phone ?: "Chưa có số điện thoại"
            tvTenantCount.text = "${item.tenantCount ?: 0} người"

            // === Giá thuê phòng ===
            tvRent.text = if (item.baseRent != null && item.baseRent > 0)
                "${item.baseRent} đ/tháng" else "Chưa có giá"
            btnEditPrice.setOnClickListener { onEditRoomPriceClick(item.id, item.baseRent) }

            // === Hiển thị toàn bộ dịch vụ + nút chỉnh giá ===
            layoutServices.removeAllViews()
            activeServices.forEach { (name, enabled, price) ->
                if (!enabled) return@forEach

                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(4, 6, 4, 6)
                }

                val tvName = TextView(ctx).apply {
                    text = "• $name"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(ctx, R.color.bsm_text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvPrice = TextView(ctx).apply {
                    text = if (price > 0) "${price} đ" else "Chưa đặt giá"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(ctx, R.color.bsm_text_secondary))
                }

                val btnEdit = ImageButton(ctx).apply {
                    setImageResource(R.drawable.ic_edit)
                    background = null
                    setColorFilter(ContextCompat.getColor(ctx, R.color.bsm_muted))
                    setOnClickListener { showServiceEditDialog(db, name, price) }
                }

                row.addView(tvName)
                row.addView(tvPrice)
                row.addView(btnEdit)
                layoutServices.addView(row)
            }

            chipOccupied.isVisible = item.status == "OCCUPIED"
            chipEmpty.isVisible = item.status == "EMPTY"

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

        private fun showServiceEditDialog(db: DatabaseHelper, serviceName: String, oldPrice: Int) {
            val ctx = itemView.context
            val input = EditText(ctx).apply {
                hint = "Nhập giá mới cho $serviceName"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(if (oldPrice > 0) oldPrice.toString() else "")
                setPadding(40, 40, 40, 40)
            }

            AlertDialog.Builder(ctx)
                .setTitle("Chỉnh giá dịch vụ")
                .setView(input)
                .setPositiveButton("Lưu") { _, _ ->
                    val newPrice = input.text.toString().toIntOrNull()
                    if (newPrice != null && newPrice >= 0) {
                        val cv = ContentValues().apply { put("price", newPrice) }
                        db.writableDatabase.update("services", cv, "name=?", arrayOf(serviceName))
                        Toast.makeText(ctx, "Đã cập nhật $serviceName: $newPrice đ", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Giá không hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    class Diff : DiffUtil.ItemCallback<UiRoom>() {
        override fun areItemsTheSame(oldItem: UiRoom, newItem: UiRoom) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UiRoom, newItem: UiRoom) = oldItem == newItem
    }
}
