package com.example.bsm_management.ui.contract

data class ContractListItem(
    val id: Int,
    val roomName: String,
    val status: String,
    val rent: String,
    val deposit: String,
    val createdDate: String,
    val endDate: String
)
