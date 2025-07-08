package com.example.wehomework.application.port.`in`

data class ApplyPointCommand(
    val userId: Long,
    val order: Long = 0L,
    val amount: Long = 0L,
) {
}