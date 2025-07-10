package com.example.wehomework

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractTestContainersIntegrationTest {
    
    companion object {
        private const val MYSQL_VERSION = "mysql:8.0.33"
        private const val REDIS_VERSION = "redis:7.0.11-alpine"
        private const val REDIS_PORT = 6379

        private val mysqlContainer: MySQLContainer<*> = MySQLContainer<Nothing>(DockerImageName.parse(MYSQL_VERSION))
            .apply {
                withDatabaseName("testdb")
                withUsername("testuser")
                withPassword("testpass")
                withReuse(true)
                withInitScript("schema.sql")
                start()
            }

        private val redisContainer: GenericContainer<*> = GenericContainer<Nothing>(DockerImageName.parse(REDIS_VERSION))
            .apply {
                withExposedPorts(REDIS_PORT)
                withReuse(true)
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(MySQLContainer.MYSQL_PORT)}/testdb"
            }
            registry.add("spring.r2dbc.username") { mysqlContainer.username }
            registry.add("spring.r2dbc.password") { mysqlContainer.password }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_PORT) }
            registry.add("logging.level.org.springframework.r2dbc") { "DEBUG" }
        }

        fun getMysqlJdbcUrl(): String = mysqlContainer.jdbcUrl
        fun getRedisHost(): String = redisContainer.host
        fun getRedisPort(): Int = redisContainer.getMappedPort(REDIS_PORT)
    }
}
