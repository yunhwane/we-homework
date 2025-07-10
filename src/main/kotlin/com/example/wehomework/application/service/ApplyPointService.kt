package com.example.wehomework.application.service

import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.application.port.`in`.ApplyPointUseCase
import com.example.wehomework.application.port.out.PointOrderCounterPort
import com.example.wehomework.application.port.out.SavePointPort
import com.example.wehomework.domain.ApplyPointResult
import com.example.wehomework.domain.calculatePoints
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Service
class ApplyPointService(
    private val pointOrderCounterPort: PointOrderCounterPort,
    private val savePointPort: SavePointPort,
    private val pointErrorHandler: PointErrorHandler,
    private val retrySpec: Retry
) : ApplyPointUseCase {

    private val logger = LoggerFactory.getLogger(ApplyPointService::class.java)


    override fun apply(command: ApplyPointCommand): Mono<ApplyPointResult> {
        return pointOrderCounterPort.getOrderAndMarkUser(command.userId)
            .flatMap { order ->
                pointErrorHandler.handleOrderResult(order, command.userId) { validOrder ->
                    applyOrder(command, validOrder)
                        .onErrorResume { dbError ->
                            rollbackOnFailure(command.userId, validOrder, dbError)
                        }
                }
            }
            .timeout(Duration.ofSeconds(10))
            .doOnError { error ->
                logger.error("Unexpected error for userId=${command.userId}", error)
            }
    }

    private fun rollbackOnFailure(userId: Long, order: Long, originalError: Throwable): Mono<ApplyPointResult> {
        return pointOrderCounterPort.rollbackUserApplication(userId, order)
            .doOnNext { rollbackSuccess ->
                if (!rollbackSuccess) {
                    logger.error("Rollback failed: userId=$userId, order=$order")
                }
            }
            .then(Mono.error(RuntimeException("서버 내부 에러로 실패하였습니다.", originalError)))
    }

    private fun applyOrder(command: ApplyPointCommand, order: Long): Mono<ApplyPointResult> {
        val amount = calculatePoints(order)
        val applyCommand = ApplyPointCommand(
            userId = command.userId,
            amount = amount,
            order = order
        )
        return savePointPort.save(applyCommand)
            .retryWhen(retrySpec)
    }
}
