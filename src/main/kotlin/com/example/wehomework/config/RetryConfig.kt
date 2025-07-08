package com.example.wehomework.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
class RetryConfig {

    @Bean
    fun databaseRetrySpec(): Retry {
        return Retry.backoff(3, Duration.ofMillis(100))
            .maxBackoff(Duration.ofSeconds(1))
            .filter { error ->
                error is org.springframework.dao.DataAccessException ||
                error.message?.contains("timeout") == true ||
                error.message?.contains("connection") == true
            }
            .doBeforeRetry { retrySignal ->
                println("DB 저장 재시도: ${retrySignal.totalRetries() + 1}/3")
            }
    }
}
