package com.example.wehomework.adapter.out.persistent


import com.example.wehomework.adapter.out.persistent.repository.PointOrderCounterRepository
import com.example.wehomework.adapter.out.persistent.repository.PointRepository
import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.application.port.out.PointOrderCounterPort
import com.example.wehomework.application.port.out.SavePointPort
import com.example.wehomework.domain.ApplyPointResult
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PointPersistentAdapter(
    private val pointRepository: PointRepository,
    private val pointOrderCounterRepository: PointOrderCounterRepository
): PointOrderCounterPort, SavePointPort {

    override fun getOrderAndMarkUser(userId: Long): Mono<Long> {
        return pointOrderCounterRepository.getOrderAndMarkUser(userId)
    }

    override fun rollbackUserApplication(userId: Long, order: Long): Mono<Boolean> {
        return pointOrderCounterRepository.rollbackUserApplication(userId, order)
    }

    override fun save(applyPointCommand: ApplyPointCommand): Mono<ApplyPointResult> {
        return pointRepository.apply(applyPointCommand)
    }

}
