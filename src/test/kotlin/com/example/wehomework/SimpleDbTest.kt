package com.example.wehomework

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
])
class SimpleDbTest {

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Test
    fun `기본 DB 연결 확인`() {
        println("🔍 기본 DB 연결 테스트")
        
        try {
            val result = databaseClient.sql("SELECT 1 as test")
                .fetch()
                .first()
                .block()
            
            println("✅ DB 연결 성공: $result")
            
        } catch (e: Exception) {
            println("❌ DB 연결 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `테이블 생성 및 삽입 테스트`() {
        println("🏗️ 테이블 생성 및 삽입 테스트")
        
        try {
            // 1. 테이블 삭제 (있다면)
            databaseClient.sql("DROP TABLE IF EXISTS points")
                .then()
                .block()
            println("🗑️ 기존 테이블 삭제 완료")
            
            // 2. 테이블 생성
            databaseClient.sql("""
                CREATE TABLE points (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    amount INT NOT NULL,
                    order_num BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            .then()
            .block()
            println("🏗️ 테이블 생성 완료")
            
            // 3. 유니크 인덱스 생성
            databaseClient.sql("CREATE UNIQUE INDEX uk_user_id ON points(user_id)")
                .then()
                .block()
            println("🔑 유니크 인덱스 생성 완료")
            
            // 4. 테스트 데이터 삽입
            val insertedRows = databaseClient.sql("""
                INSERT INTO points (user_id, amount, order_num)
                VALUES (?, ?, ?)
            """)
            .bind(0, 12345L)
            .bind(1, 500)
            .bind(2, 1L)
            .fetch()
            .rowsUpdated()
            .block()
            
            println("✅ 데이터 삽입 완료: $insertedRows 행")
            
            // 5. 데이터 조회
            val selectResult = databaseClient.sql("SELECT * FROM points WHERE user_id = ?")
                .bind(0, 12345L)
                .fetch()
                .first()
                .block()
            
            println("📋 삽입된 데이터: $selectResult")
            
            // 6. 정리
            databaseClient.sql("DELETE FROM points WHERE user_id = ?")
                .bind(0, 12345L)
                .fetch()
                .rowsUpdated()
                .block()
            
            println("🧹 테스트 데이터 정리 완료")
            
        } catch (e: Exception) {
            println("❌ 테이블 테스트 실패: ${e.javaClass.simpleName}")
            println("에러 메시지: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `Named Parameter 방식 테스트`() {
        println("🏷️ Named Parameter 방식 테스트")
        
        try {
            // 테이블이 있는지 확인
            val tableExists = databaseClient.sql("""
                SELECT COUNT(*) as count 
                FROM INFORMATION_SCHEMA.TABLES 
                WHERE TABLE_NAME = 'POINTS'
            """)
            .fetch()
            .first()
            .block()
            
            println("📊 테이블 존재 확인: $tableExists")
            
            if (tableExists?.get("count") == 0L) {
                // 테이블 생성
                databaseClient.sql("""
                    CREATE TABLE points (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        amount INT NOT NULL,
                        order_num BIGINT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                .then()
                .block()
                println("🏗️ 테이블 새로 생성함")
            }
            
            // Named parameter로 삽입 테스트
            val insertResult = databaseClient.sql("""
                INSERT INTO points (user_id, amount, order_num)
                VALUES (:userId, :amount, :orderNum)
            """)
            .bind("userId", 54321L)
            .bind("amount", 1000)
            .bind("orderNum", 2L)
            .fetch()
            .rowsUpdated()
            .block()
            
            println("✅ Named Parameter 삽입 성공: $insertResult 행")
            
            // 조회 확인
            val selectResult = databaseClient.sql("SELECT * FROM points WHERE user_id = :userId")
                .bind("userId", 54321L)
                .fetch()
                .first()
                .block()
            
            println("📋 Named Parameter 조회 결과: $selectResult")
            
            // 정리
            databaseClient.sql("DELETE FROM points WHERE user_id = :userId")
                .bind("userId", 54321L)
                .fetch()
                .rowsUpdated()
                .block()
            
            println("🧹 Named Parameter 테스트 완료")
            
        } catch (e: Exception) {
            println("❌ Named Parameter 테스트 실패: ${e.javaClass.simpleName}")
            println("에러 메시지: ${e.message}")
            e.printStackTrace()
        }
    }
}
