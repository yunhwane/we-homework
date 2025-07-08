package com.example.wehomework.domain

import java.time.LocalDateTime

class ApplyPointResult (
    val order: Long,
    val amount: Long,
    val userId: Long,
    val createdAt: LocalDateTime? = null,
) {
}