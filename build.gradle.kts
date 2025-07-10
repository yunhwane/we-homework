plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    
    runtimeOnly("io.asyncer:r2dbc-mysql:1.0.5")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
    
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:mysql")
    
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.4.0")
    testImplementation("io.rest-assured:json-path:5.4.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    jvmArgs = listOf(
        "-Xmx6g", "-Xms3g",
        "-XX:NewRatio=2",
        "-XX:MaxMetaspaceSize=256m",

        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-XX:G1HeapRegionSize=16m",

        "-XX:+UseStringDeduplication",
        "-XX:+OptimizeStringConcat",

        "-Dio.netty.leakDetection.level=disabled",
        "-Dio.netty.allocator.numDirectArenas=8",
        "-Dio.netty.allocator.numHeapArenas=8",

        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8"
    )
    
    systemProperty("junit.jupiter.execution.timeout.default", "15m")
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
}
