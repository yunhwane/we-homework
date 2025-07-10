package com.example.wehomework.config

import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {
    
    @Bean
    fun lettuceClientResources(): ClientResources {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(16)           // IO 스레드 풀 크기 증가
            .computationThreadPoolSize(16)  // 연산 스레드 풀 크기 증가
            .build()
    }
    
    @Bean
    fun lettuceClientConfiguration(clientResources: ClientResources): LettuceClientConfiguration {
        val socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(2))
            .keepAlive(true)
            .tcpNoDelay(true)
            .build()
            
        val clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
            .build()
            
        return LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .clientResources(clientResources)
            .commandTimeout(Duration.ofSeconds(3))
            .build()
    }
    
    @Bean
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = StringRedisSerializer()
        val hashKeySerializer = StringRedisSerializer()
        val hashValueSerializer = StringRedisSerializer()
        
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, String>()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(hashKeySerializer)
            .hashValue(hashValueSerializer)
            .build()
            
        return ReactiveRedisTemplate(factory, serializationContext)
    }
}
