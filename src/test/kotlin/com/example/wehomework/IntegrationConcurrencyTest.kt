package com.example.wehomework

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "logging.level.com.example.wehomework=INFO"
])
class IntegrationConcurrencyTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var redisTemplate: ReactiveStringRedisTemplate

    private val userIdGenerator = AtomicLong(System.currentTimeMillis() / 1000)

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        redisTemplate.delete("point:order:counter", "point:applied:users").block()
    }

    @Test
    fun `RestAssured로 10000명 동시 접속 테스트`() {
        val totalUsers = 10_000
        val concurrency = 100
        val testResults = ConcurrentTestResults()

        val userIds = generateUniqueUserIds(totalUsers)
        
        val duration = measureTimeMillis {
            runConcurrentTest(userIds, concurrency, testResults)
        }

        testResults.printResults("RestAssured 동시성 테스트", totalUsers, duration)
        assert(testResults.successCount.get() <= 10_000) { "성공 수가 최대 참가자 수를 초과했습니다." }
        assert(testResults.successCount.get() > 9_500) { "성공률이 너무 낮습니다: ${testResults.successCount.get()}" }
    }

    @Test
    fun `RestAssured로 순차 마감 테스트`() {
        val totalUsers = 10_001
        val testResults = ConcurrentTestResults()

        val userIds = generateUniqueUserIds(totalUsers, 50_000)
        
        val duration = measureTimeMillis {
            runSequentialTest(userIds, testResults)
        }

        testResults.printResults("RestAssured 순차 마감 테스트", totalUsers, duration)

        // 검증
        assert(testResults.successCount.get() == 10_000) { "정확히 10,000명이 성공해야 합니다: ${testResults.successCount.get()}" }
        assert(testResults.hasClosureSign()) { "마감 표시가 없습니다." }
    }

    @Test
    fun `RestAssured 부하 단계별 테스트`() {
        val stages = listOf(
            TestStage("1단계 - 웜업", 1_000, 50),
            TestStage("2단계 - 중간 부하", 3_000, 80),
            TestStage("3단계 - 고부하", 7_000, 120),
            TestStage("4단계 - 최대 부하", 10_000, 150)
        )

        stages.forEach { stage ->
            redisTemplate.delete("point:order:counter", "point:applied:users").block()

            val testResults = ConcurrentTestResults()
            val userIds = generateUniqueUserIds(stage.userCount, stage.userCount * 1000L)

            val duration = measureTimeMillis {
                runConcurrentTest(userIds, stage.concurrency, testResults)
            }

            testResults.printResults(stage.name, stage.userCount, duration)
            
            val throughput = stage.userCount * 1000.0 / duration
            println("단계 결과: ${"%.2f".format(throughput)} req/s")

            Thread.sleep(3000)
        }
    }

    @Test
    fun `RestAssured 성능 벤치마크 테스트`() {
        val benchmarks = listOf(
            50 to 10,
            100 to 20,
            200 to 30,
            300 to 40
        )

        val results = mutableListOf<BenchmarkResult>()

        benchmarks.forEach { (concurrency, _) ->
            redisTemplate.delete("point:order:counter", "point:applied:users").block()
            
            val userCount = 5_000
            val testResults = ConcurrentTestResults()
            val userIds = generateUniqueUserIds(userCount, concurrency * 10_000L)

            val duration = measureTimeMillis {
                runConcurrentTest(userIds, concurrency, testResults)
            }

            val throughput = userCount * 1000.0 / duration
            val successRate = testResults.successCount.get() * 100.0 / userCount

            results.add(BenchmarkResult(
                concurrency = concurrency,
                throughput = throughput,
                successRate = successRate,
                avgResponseTime = testResults.getAverageResponseTime()
            ))

            println("동시성 $concurrency: ${"%.2f".format(throughput)} req/s, 성공률: ${"%.2f".format(successRate)}%")
        }

        printBenchmarkSummary(results)
    }

    // === 유틸리티 메서드들 ===

    private fun generateUniqueUserIds(count: Int, offset: Long = 0): List<Long> {
        val baseId = userIdGenerator.addAndGet(offset + count)
        return (baseId until baseId + count).toList()
    }

    private fun runConcurrentTest(userIds: List<Long>, concurrency: Int, results: ConcurrentTestResults) {
        val executor = Executors.newFixedThreadPool(concurrency)
        val latch = CountDownLatch(userIds.size)

        try {
            userIds.forEach { userId ->
                executor.submit {
                    try {
                        val startTime = System.currentTimeMillis()
                        val response = applyPointWithRestAssured(userId)
                        val responseTime = System.currentTimeMillis() - startTime
                        
                        results.recordResponse(response, responseTime)
                    } catch (e: Exception) {
                        results.recordException(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.MINUTES)
        } finally {
            executor.shutdown()
        }
    }

    private fun runSequentialTest(userIds: List<Long>, results: ConcurrentTestResults) {
        userIds.forEachIndexed { index, userId ->
            try {
                val startTime = System.currentTimeMillis()
                val response = applyPointWithRestAssured(userId)
                val responseTime = System.currentTimeMillis() - startTime
                
                results.recordResponse(response, responseTime)

                if ((index + 1) % 1000 == 0) {
                    println("진행률: ${index + 1}/${userIds.size}")
                }
            } catch (e: Exception) {
                results.recordException(e)
            }
        }
    }

    private fun applyPointWithRestAssured(userId: Long): Response {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"userId": $userId}""")
            .`when`()
            .post("/api/v1/point/apply")
            .then()
            .extract()
            .response()
    }

    private fun printBenchmarkSummary(results: List<BenchmarkResult>) {
        println("\n${"=".repeat(80)}")
        println("${"=".repeat(80)}")
        println("%-10s %15s %12s %15s".format("동시성", "처리량(req/s)", "성공률(%)", "평균응답(ms)"))
        println("-".repeat(80))

        results.forEach { result ->
            println("%-10d %15.2f %12.2f %15.2f".format(
                result.concurrency,
                result.throughput,
                result.successRate,
                result.avgResponseTime
            ))
        }
        println("${"=".repeat(80)}")
    }
}

class ConcurrentTestResults {
    val successCount = AtomicInteger(0)
    val duplicateCount = AtomicInteger(0)
    val closedCount = AtomicInteger(0)
    val errorCount = AtomicInteger(0)
    val exceptionCount = AtomicInteger(0)
    private val responseTimes = ConcurrentLinkedQueue<Long>()
    private val errorMessages = ConcurrentHashMap<String, AtomicInteger>()

    fun recordResponse(response: Response, responseTime: Long) {
        responseTimes.offer(responseTime)
        
        when (response.statusCode) {
            200 -> {
                val body = response.body.asString()
                if (body.contains("\"success\":true")) {
                    val count = successCount.incrementAndGet()
                    if (count % 1000 == 0) print("1")
                } else {
                    recordError(body)
                }
            }
            400 -> {
                val body = response.body.asString()
                recordError(body)
            }
            else -> {
                errorCount.incrementAndGet()
                print(0)
            }
        }
    }

    private fun recordError(message: String) {
        when {
            message.contains("이미 신청한") -> {
                duplicateCount.incrementAndGet()
                print("-1")
            }
            message.contains("마감") -> {
                closedCount.incrementAndGet()
                print("-2")
            }
            else -> {
                errorCount.incrementAndGet()
                errorMessages.computeIfAbsent(message.take(50)) { AtomicInteger(0) }.incrementAndGet()
                print("-3")
            }
        }
    }

    fun recordException(exception: Exception) {
        exceptionCount.incrementAndGet()
        errorMessages.computeIfAbsent("Exception: ${exception.message}".take(50)) { AtomicInteger(0) }.incrementAndGet()
        print("💥")
    }

    fun hasClosureSign(): Boolean {
        return closedCount.get() > 0 || exceptionCount.get() > 0
    }

    fun getAverageResponseTime(): Double {
        return if (responseTimes.isNotEmpty()) {
            responseTimes.average()
        } else 0.0
    }

    fun printResults(testName: String, totalRequests: Int, duration: Long) {
        println("\n")
        println("=== $testName 결과 ===")
        println("총 요청 수: $totalRequests")
        println("성공: ${successCount.get()}")
        println("중복: ${duplicateCount.get()}")
        println("마감: ${closedCount.get()}")
        println("기타 실패: ${errorCount.get()}")
        println("Exception: ${exceptionCount.get()}")
        println("총 처리 시간: ${duration}ms")
        println("초당 처리량: ${"%.2f".format(totalRequests * 1000.0 / duration)} req/s")
        println("성공률: ${"%.2f".format(successCount.get() * 100.0 / totalRequests)}%")
        
        if (responseTimes.isNotEmpty()) {
            println("평균 응답시간: ${"%.2f".format(getAverageResponseTime())}ms")
        }

        if (errorMessages.isNotEmpty()) {
            println("\n=== 에러 상세 ===")
            errorMessages.entries.take(3).forEach { (message, count) ->
                println("$message: ${count.get()}회")
            }
        }
    }
}

data class TestStage(
    val name: String,
    val userCount: Int,
    val concurrency: Int
)

data class BenchmarkResult(
    val concurrency: Int,
    val throughput: Double,
    val successRate: Double,
    val avgResponseTime: Double
)
