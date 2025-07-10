package com.example.wehomework

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.r2dbc.core.DatabaseClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest : AbstractTestContainersIntegrationTest() {

    companion object {
        private const val TOTAL_USERS = 10_000
        private const val CONCURRENCY = 100
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var databaseClient: DatabaseClient

    @Autowired
    lateinit var redisTemplate: ReactiveStringRedisTemplate

    private val successCount = AtomicInteger(0)
    private val failCount = AtomicInteger(0)

    @BeforeAll
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        
        println("=== 테스트 환경 정보 ===")
        println("MySQL: ${getMysqlJdbcUrl()}")
        println("Redis: ${getRedisHost()}:${getRedisPort()}")
        println("API Server: http://localhost:$port")
        println("========================")
        
        // 테이블 존재 여부 확인
        checkDatabaseConnection()
    }

    @BeforeEach
    fun cleanUp() {

        redisTemplate.delete("point:order:counter", "point:applied:users")
            .block()

        databaseClient.sql("TRUNCATE TABLE points")
            .then()
            .block()

        successCount.set(0)
        failCount.set(0)
    }

    @Test
    fun `10,000명 동시 신청 테스트`() {
        // Given
        val userIds = (1..TOTAL_USERS).map { it }
        val executor = Executors.newFixedThreadPool(CONCURRENCY)

        // When
        val duration = measureTimeMillis {
            val futures = userIds.map { userId ->
                executor.submit {
                    val success = tryRequest(userId.toLong())
                    if (success) {
                        successCount.incrementAndGet()
                    } else {
                        failCount.incrementAndGet()
                    }
                }
            }

            futures.forEach { it.get() }
            executor.shutdown()
            executor.awaitTermination(2, TimeUnit.MINUTES)
        }

        printTestResults(duration)
        
        assertThat(successCount.get())
            .isLessThanOrEqualTo(TOTAL_USERS)
            .withFailMessage("성공 수가 총 참가자 수를 초과했습니다!")
        
        assertThat(successCount.get())
            .isGreaterThanOrEqualTo(9900)
            .withFailMessage("성공률이 너무 낮습니다: ${successCount.get()}")
    }

    @Test
    fun `같은 userId가 여러 번 신청하면 첫 번째만 성공해야 한다`() {
        val userId = 10_000L

        val firstResponse = sendPointApplyRequest(userId)
        assertThat(firstResponse.statusCode).isEqualTo(200)
        assertThat(firstResponse.body.asString()).contains("success")

        val secondResponse = sendPointApplyRequest(userId)

        assertThat(secondResponse.statusCode).isEqualTo(409)
        assertThat(secondResponse.body.asString().lowercase()).contains("duplicate")
    }

    private fun tryRequest(userId: Long): Boolean {
        return try {
            val response = sendPointApplyRequest(userId)
            response.statusCode == 200 && response.body.asString().contains("success")
        } catch (e: Exception) {
            println("Request failed for userId $userId: ${e.message}")
            false
        }
    }

    private fun sendPointApplyRequest(userId: Long): Response {
        return RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(mapOf("userId" to userId))
            .`when`()
            .post("/api/v1/point/apply")
            .then()
            .extract()
            .response()
    }

    private fun checkDatabaseConnection() {
        try {
            val count = databaseClient.sql("SELECT COUNT(*) as count FROM points")
                .fetch()
                .first()
                .block()

            println("데이터베이스 연결 확인: points 테이블에 ${count?.get("count")}개의 레코드가 있습니다.")
        } catch (e: Exception) {
            println("데이터베이스 연결 실패: ${e.message}")
            throw e
        }
    }

    private fun printTestResults(duration: Long) {
        println("=== 동시성 테스트 결과 ===")
        println("소요 시간: $duration ms")
        println("성공: ${successCount.get()}")
        println("실패: ${failCount.get()}")
        println("성공률: ${(successCount.get().toDouble() / TOTAL_USERS * 100).format(2)}%")
        println("========================")
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
