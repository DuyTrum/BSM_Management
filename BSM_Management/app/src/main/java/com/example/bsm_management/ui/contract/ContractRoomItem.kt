package com.example.bsm_management.ui.contract

data class RoomCardItem(
    val roomName: String,
    val price: String,
    val isEmpty: Boolean = false,       // hiển thị chip "Đang trống"
    val waitNextCycle: Boolean = false  // hiển thị chip "Chờ kỳ thu tới"
)