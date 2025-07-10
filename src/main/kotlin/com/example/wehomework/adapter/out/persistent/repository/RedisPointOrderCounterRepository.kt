package com.example.wehomework.adapter.out.persistent.repository

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono


@Repository
class RedisPointOrderCounterRepository(
    private val redisTemplate: ReactiveStringRedisTemplate
) : PointOrderCounterRepository {


    override fun getOrderAndMarkUser(userId: Long): Mono<Long> {
        return redisTemplate.execute(
            APPLY_SCRIPT,
            listOf(ORDER_COUNTER_KEY, USER_SET_KEY),
            userId.toString(), MAX_PARTICIPANTS.toString()).next()
    }

    // DB 저장 실패 시 Redis 데이터 롤백
    override fun rollbackUserApplication(userId: Long, order: Long): Mono<Boolean> {
        return redisTemplate.execute(
            ROLLBACK_SCRIPT,
            listOf(ORDER_COUNTER_KEY, USER_SET_KEY),
            userId.toString(), order.toString()
        ).next()
    }

    companion object {
        const val ORDER_COUNTER_KEY = "point:order:counter"
        const val USER_SET_KEY = "point:applied:users"
        const val MAX_PARTICIPANTS = 10_000L

        private val APPLY_SCRIPT = RedisScript.of<Long>("""
            local userId = ARGV[1]
            local maxCount = tonumber(ARGV[2])

            -- 유저가 이미 신청했는지 확인
            local userExists = redis.call('SISMEMBER', KEYS[2], userId)
            if userExists == 1 then
                 return -1  -- 이미 신청한 사용자
            end

            -- 현재 순서 확인
            local currentOrder = redis.call('GET', KEYS[1])
            local orderNum = tonumber(currentOrder) or 0
            if orderNum >= maxCount then
                    return -2  -- 인원 초과
            end

            -- 순서 증가 및 사용자 등록
            local newOrder = redis.call('INCR', KEYS[1])
            redis.call('SADD', KEYS[2], userId)

            return newOrder
        """.trimIndent(), Long::class.java)

        // 롤백 스크립트
        private val ROLLBACK_SCRIPT = RedisScript.of<Boolean>("""
            local userId = ARGV[1]
            local order = tonumber(ARGV[2])
            
            -- 사용자가 실제로 신청했는지 확인
            if redis.call('SISMEMBER', KEYS[2], userId) == 0 then
                return false  -- 신청하지 않은 사용자
            end
            
            -- 현재 카운터가 롤백하려는 순서와 일치하는지 확인
            local currentOrder = redis.call('GET', KEYS[1])
            if currentOrder == false or tonumber(currentOrder) ~= order then
                return false  -- 순서가 맞지 않음
            end
            
            -- 롤백 실행: 카운터 감소 및 사용자 제거
            redis.call('DECR', KEYS[1])
            redis.call('SREM', KEYS[2], userId)
            
            return true
        """.trimIndent(), Boolean::class.java)
    }
}