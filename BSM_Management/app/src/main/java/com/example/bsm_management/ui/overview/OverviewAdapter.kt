package com.example.bsm_management.ui.overview

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R

class OverviewAdapter(
    private val onClick: (OverviewStat) -> Unit = {}
) : ListAdapter<OverviewStat, OverviewAdapter.VH>(DIFF) {


    object DIFF : DiffUtil.ItemCallback<OverviewStat>() {
        override fun areItemsTheSame(o: OverviewStat, n: OverviewStat) = o.title == n.title
        override fun areContentsTheSame(o: OverviewStat, n: OverviewStat) = o == n
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvValue: TextView = v.findViewById(R.id.tvValue)
        val tvPercent: TextView = v.findViewById(R.id.tvPercent)
        init {
            v.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(getItem(pos))
                }
            }
        }

    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_overview, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {

        val it = getItem(pos)
        h.ivIcon.setImageResource(it.iconRes)
        h.tvTitle.text = it.title
        h.tvValue.text = it.value.toString()
        h.tvPercent.text = "${it.percent}%"
        (h.tvPercent.background as? GradientDrawable)?.setColor(it.badgeColor)
    }
}
