package com.example.bsm_management.ui.overview

data class OverviewStat(
    val title: String,
    val value: Int,
    val percent: Int,
    val iconRes: Int,
    val badgeColor: Int    // màu nền cho % (ARGB), ví dụ Color.parseColor("#FFF2EE")
)
