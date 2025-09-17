package com.example.bsm_management.ui.dashboard


data class ActionItem(
    val iconRes: Int,
    val title: String,
    val subtitle: String? = null,
    val badge: Int? = null
)