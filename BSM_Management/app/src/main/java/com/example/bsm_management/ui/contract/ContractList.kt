package com.example.bsm_management.ui.contract

data class Contract(
    val roomName: String,
    val contractCode: String,
    val status: String,
    val rent: String,
    val deposit: String,
    val collected: String,
    val createdDate: String,
    val moveInDate: String,
    val endDate: String
)