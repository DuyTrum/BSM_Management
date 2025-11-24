package com.example.bsm_management.ui.tenant

data class Tenant(
    val id: Int,
    val name: String,
    val phone: String,
    val roomId: Int?,
    val cccd: String? = null,
    val address: String? = null,
    val dob: Long? = null,
    val slotIndex: Int,
    val isOld: Boolean = false
)
