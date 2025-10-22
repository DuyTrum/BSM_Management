package com.example.bsm_management.ui.model

data class MessageItem(
    val id: Long,
    val title: String,
    val content: String?,
    val tag: String?,
    val pinned: Boolean,
    val unread: Boolean,
    val createdAt: Long
)
