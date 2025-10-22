package com.example.bsm_management.ui.contract

data class ContractRoomItem(
    val roomId: Int,
    val roomName: String,
    val price: String,
    val isEmpty: Boolean = false,
    val waitNextCycle: Boolean = false
)
