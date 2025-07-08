package com.example.wehomework.adapter.out.persistent.repository

import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.domain.ApplyPointResult
import reactor.core.publisher.Mono

interface PointRepository {
    fun apply(applyPointCommand: ApplyPointCommand): Mono<ApplyPointResult>
}