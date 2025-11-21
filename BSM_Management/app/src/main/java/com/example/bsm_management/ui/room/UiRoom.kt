package com.example.bsm_management.ui.room

data class UiRoom(
    val id: Long,
    val name: String,
    val baseRent: Int?,
    val floor: Int?,
    val status: String?,
    val tenantCount: Int?,
    val maxPeople: Int?,
    val contractEnd: String?,
    val appUsed: Boolean?,
    val onlineSigned: Boolean?,
    val phone: String?,
    val services: List<Triple<String, Boolean, Int>> = emptyList()


)
