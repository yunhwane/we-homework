package com.example.wehomework.application.port.`in`

import com.example.wehomework.domain.ApplyPointResult
import reactor.core.publisher.Mono

interface ApplyPointUseCase {
    fun apply(command: ApplyPointCommand): Mono<ApplyPointResult>
}