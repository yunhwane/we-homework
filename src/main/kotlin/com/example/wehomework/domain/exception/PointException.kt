package com.example.wehomework.domain.exception

sealed class PointException(message: String) : RuntimeException(message)

class DuplicateUserException(userId: Long) : PointException("이미 신청한 사용자입니다. (userId: $userId)")

class ApplicationClosedException : PointException("신청 마감되었습니다.")

class MaxParticipantsExceededException(order: Long, maxParticipants: Long) : 
    PointException("최대 참가자 수를 초과했습니다. (순서: $order, 최대: $maxParticipants)")
