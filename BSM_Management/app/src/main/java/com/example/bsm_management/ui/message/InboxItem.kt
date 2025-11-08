package com.example.bsm_management.ui.message

data class InboxItem(
    val hostelName: String,
    val sender: String,
    val message: String,
    val time: String,
    val isRead: Boolean
)
