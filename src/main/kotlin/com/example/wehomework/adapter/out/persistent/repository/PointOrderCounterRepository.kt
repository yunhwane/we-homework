package com.example.wehomework.adapter.out.persistent.repository

import reactor.core.publisher.Mono


interface PointOrderCounterRepository {
    fun getOrderAndMarkUser(userId: Long): Mono<Long>
    fun rollbackUserApplication(userId: Long, order: Long): Mono<Boolean>
}