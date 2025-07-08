package com.example.wehomework.application.port.out

import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.domain.ApplyPointResult
import reactor.core.publisher.Mono

interface SavePointPort {
    fun save(applyPointCommand: ApplyPointCommand): Mono<ApplyPointResult>
}