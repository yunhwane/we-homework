package com.example.wehomework.adapter.`in`.web

import com.example.wehomework.domain.exception.ApplicationClosedException
import com.example.wehomework.domain.exception.DuplicateUserException
import com.example.wehomework.domain.exception.MaxParticipantsExceededException
import com.example.wehomework.domain.exception.PointException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Mono

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DuplicateUserException::class)
    fun handleDuplicateUserException(ex: DuplicateUserException): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ErrorResponse(
                        success = false,
                        message = ex.message ?: "이미 신청한 사용자입니다.",
                        errorCode = "DUPLICATE_USER",
                        data = null
                    )
                )
        )
    }

    @ExceptionHandler(ApplicationClosedException::class)
    fun handleApplicationClosedException(ex: ApplicationClosedException): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ErrorResponse(
                        success = false,
                        message = ex.message ?: "신청 마감되었습니다.",
                        errorCode = "APPLICATION_CLOSED",
                        data = null
                    )
                )
        )
    }

    @ExceptionHandler(MaxParticipantsExceededException::class)
    fun handleMaxParticipantsExceededException(ex: MaxParticipantsExceededException): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ErrorResponse(
                        success = false,
                        message = ex.message ?: "최대 참가자 수를 초과했습니다.",
                        errorCode = "MAX_PARTICIPANTS_EXCEEDED",
                        data = null
                    )
                )
        )
    }

    @ExceptionHandler(PointException::class)
    fun handlePointException(ex: PointException): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ErrorResponse(
                        success = false,
                        message = ex.message ?: "포인트 처리 중 오류가 발생했습니다.",
                        errorCode = "POINT_ERROR",
                        data = null
                    )
                )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): Mono<ResponseEntity<ErrorResponse>> {
        log.info("Unhandled exception occurred: {}", ex.message, ex)
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ErrorResponse(
                        success = false,
                        message = "서버 내부 오류가 발생했습니다.",
                        errorCode = "INTERNAL_SERVER_ERROR",
                        data = null
                    )
                )
        )
    }
}

