package com.example.wehomework.application.port.out

import reactor.core.publisher.Mono

interface PointOrderCounterPort {
    fun getOrderAndMarkUser(userId: Long): Mono<Long>
    fun rollbackUserApplication(userId: Long, order: Long): Mono<Boolean>
}