package com.example.wehomework.adapter.out.persistent.repository

import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.domain.ApplyPointResult
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono


@Repository
class R2DBCPointRepository(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator
) : PointRepository{

    override fun apply(applyPointCommand: ApplyPointCommand): Mono<ApplyPointResult> {
        return Mono.fromCallable {
            applyPointCommand
        }
        .flatMap { command ->
            checkDuplicateApply(command.userId)
                .flatMap { exists ->
                    if (exists) {
                        Mono.error(IllegalArgumentException("Point for user ${command.userId} already exists."))
                    } else {
                        createPoint(command)
                    }
                }
        }
        .`as`(transactionalOperator::transactional)
    }

    private fun createPoint(applyPointCommand: ApplyPointCommand): Mono<ApplyPointResult> {
        return databaseClient.sql(INSERT_POINT_QUERY)
            .bind("userId", applyPointCommand.userId)
            .bind("amount", applyPointCommand.amount)
            .bind("orderNum", applyPointCommand.order)  // 파라미터명 수정
            .fetch()
            .rowsUpdated()
            .doOnNext { rowsUpdated ->
            }
            .doOnError { error ->
                error.printStackTrace()
            }
            .map {
                ApplyPointResult(
                    userId = applyPointCommand.userId,
                    amount = applyPointCommand.amount,
                    order = applyPointCommand.order
                )
            }
    }

    private fun checkDuplicateApply(userId: Long): Mono<Boolean> {
        return databaseClient.sql(SELECT_POINT_QUERY)
            .bind("userId", userId)
            .fetch()
            .first()
            .map { true }
            .switchIfEmpty(Mono.just(false))
    }

    companion object {
        private var INSERT_POINT_QUERY = """
            INSERT INTO points (user_id, amount, order_num)
            VALUES (:userId, :amount, :orderNum)
        """.trimIndent()

        private var SELECT_POINT_QUERY = """
            SELECT user_id FROM points
            WHERE user_id = :userId
        """.trimIndent()
    }
}