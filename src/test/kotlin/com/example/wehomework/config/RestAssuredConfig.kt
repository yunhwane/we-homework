package com.example.wehomework.config

import io.restassured.RestAssured
import io.restassured.config.ConnectionConfig
import io.restassured.config.HttpClientConfig
import io.restassured.config.RestAssuredConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.junit.jupiter.api.BeforeAll
import java.util.concurrent.TimeUnit

class RestAssuredTestConfig {
    
    companion object {
        @JvmStatic
        @BeforeAll
        fun setupRestAssured() {
            // 커넥션 풀 설정
            val connectionManager = PoolingHttpClientConnectionManager().apply {
                maxTotal = 200                    // 최대 연결 수 제한
                defaultMaxPerRoute = 50           // 라우트당 최대 연결 수
                closeIdleConnections(30, TimeUnit.SECONDS)  // 유휴 연결 정리
            }
            
            val httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setConnectionTimeToLive(60, TimeUnit.SECONDS)  // 연결 수명
                .evictIdleConnections(30, TimeUnit.SECONDS)     // 유휴 연결 제거
                .build()
            
            // RestAssured 설정
            val config = RestAssuredConfig.config()
                .connectionConfig(
                    ConnectionConfig.connectionConfig()
                        .closeIdleConnectionsAfterEachResponseAfter(5, TimeUnit.SECONDS)
                )
                .httpClient(
                    HttpClientConfig.httpClientConfig()
                        .reuseHttpClientInstance()
                        .httpClientFactory { httpClient }
                )
            
            RestAssured.config = config
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }
}
