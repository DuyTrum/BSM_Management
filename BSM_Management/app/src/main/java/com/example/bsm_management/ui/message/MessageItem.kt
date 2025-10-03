package com.example.bsm_management.ui.message

data class MessageItem(
    val sender: String,
    val content: String,
    val time: String,
    val unread: Boolean = false
)
