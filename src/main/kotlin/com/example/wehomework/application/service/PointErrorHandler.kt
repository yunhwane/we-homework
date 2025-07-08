package com.example.wehomework.application.service

import com.example.wehomework.domain.exception.ApplicationClosedException
import com.example.wehomework.domain.exception.DuplicateUserException
import com.example.wehomework.domain.exception.MaxParticipantsExceededException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PointErrorHandler {
    
    private val logger = LoggerFactory.getLogger(PointErrorHandler::class.java)
    
    companion object {
        private const val DUPLICATE_USER_CODE = -1L
        private const val APPLICATION_CLOSED_CODE = -2L
    }
    
    fun <T> handleOrderResult(order: Long, userId: Long, maxParticipants: Long, onSuccess: (Long) -> Mono<T>): Mono<T> {
        logger.debug("Handling order result: order={}, userId={}, maxParticipants={}", order, userId, maxParticipants)
        
        return when {
            order == DUPLICATE_USER_CODE -> {
                logger.warn("Duplicate user application detected: userId={}", userId)
                Mono.error(DuplicateUserException(userId))
            }
            order == APPLICATION_CLOSED_CODE -> {
                logger.warn("Application closed: userId={}", userId)
                Mono.error(ApplicationClosedException())
            }
            order > maxParticipants -> {
                logger.warn("Max participants exceeded: order={}, maxParticipants={}, userId={}", order, maxParticipants, userId)
                Mono.error(MaxParticipantsExceededException(order, maxParticipants))
            }
            order <= 0 -> {
                logger.error("Invalid order value: order={}, userId={}", order, userId)
                Mono.error(IllegalStateException("잘못된 순서 값입니다: $order"))
            }
            else -> {
                logger.info("Valid order processed: order={}, userId={}", order, userId)
                onSuccess(order)
            }
        }
    }
}
