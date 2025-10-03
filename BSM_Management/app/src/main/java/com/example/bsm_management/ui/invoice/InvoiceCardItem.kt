package com.example.bsm_management.ui.invoice

data class InvoiceItem(
    val id: String,
    val title: String,
    val mainStatus: String,
    val rent: String,
    val deposit: String,
    val collected: String,
    val createdDate: String,
    val moveInDate: String,
    val endDate: String
)
