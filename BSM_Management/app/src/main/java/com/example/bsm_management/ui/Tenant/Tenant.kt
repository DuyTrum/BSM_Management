package com.example.bsm_management.ui.tenant

data class Tenant(
    val id: Int,
    val name: String,
    val phone: String,
    val roomId: Int?,
    var isUsingApp: Boolean = false,
    var hasTemporaryResidence: Boolean = false,
    var hasEnoughDocuments: Boolean = false,
    val slotIndex: Int
)


