package com.example.bsm_management.ui.message

data class MessageItem(
    val title: String,
    val content: String,
    val time: String,
    val pinned: Boolean = false
)
