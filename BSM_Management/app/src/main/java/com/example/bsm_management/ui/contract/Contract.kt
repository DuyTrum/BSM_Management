package com.example.bsm_management.ui.contract

data class Contract(
    val id: Int = 0,
    val roomId: Int,
    val tenantName: String,
    val tenantPhone: String,
    val startDate: Long,
    val endDate: Long?,
    val deposit: Int,
    val active: Int = 1
)
