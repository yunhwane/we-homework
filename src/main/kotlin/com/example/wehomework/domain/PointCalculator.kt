package com.example.wehomework.domain

fun calculatePoints(order: Long): Long {
    return when (order) {
        in 1..100 -> 100_000L
        in 101..2_000 -> 50_000L
        in 2_001..5_000 -> 20_000L
        in 5_001..10_000 -> 10_000L
        else -> 0L
    }
}