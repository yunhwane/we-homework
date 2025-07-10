# 🎯 선착순 포인트 지급 시스템

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blue)
![Redis](https://img.shields.io/badge/Redis-Latest-red)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)
![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-purple)

**고성능 선착순 포인트 지급 시스템** - 10,000명 동시 접속 처리 가능

## 시스템 개요

## 🛠 기술 스택

### Backend

#### **Framework**: Spring Boot 3.5.3 (WebFlux)
**선택 이유:**
- **고성능 요구사항**: 10,000명 동시 접속을 처리하기 위해 **Reactive Programming**
- **Non-blocking I/O**: 전통적인 MVC보다 **10배 이상 높은 동시성** 처리 가능하기에 경량화 된 어플리케이션 효율이 좋음
- **메모리 효율성**: Thread-per-request 모델 대신 **Event Loop** 기반으로 메모리 사용량 최소화
- **Back-pressure 지원**: 과부하 상황에서 자동으로 흐름 제어

> 익숙한 Spring 생태계와의 호환성을 고려하여 WebFlux를 선택하였으며, 이를 통해 Reactive Programming을 적용하여 경량 애플리케이션에서의 처리 효율을 극대화할 수 있었습니다. 또한, Tomcat 기반보다 Netty 기반의 논블로킹 아키텍처를 활용함으로써 더 우수한 동시성 제어와 리소스 효율성을 확보하기 위해 webflux 기술을 선택했습니다.

#### **Language**: Kotlin 1.9.25
**선택 이유:**
- **Java 호환성**: 기존 Spring 생태계 완벽 활용
- **간결한 문법**: Java 대비 **30-40% 적은 코드량**으로 개발 속도 향상
- **Null Safety**: Runtime NPE 방지로 **시스템 안정성** 크게 개선
- **Coroutines**: Reactive Programming과 완벽 호환
- **Extension Functions**: 코드 가독성과 유지보수성 향상
> Kotlin을 선택한 이유는 Java와의 호환성 덕분에 기존 Spring 생태계를 그대로 활용하면서도, 간결한 문법과 Null Safety로 코드 품질을 크게 향상시킬 수 있었기 때문입니다. 또한, Coroutines를 통해 Reactive Programming을 자연스럽게 적용할 수 있어 시스템의 안정성과 성능을 동시에 확보하기 위해 사용했습니다.

#### **Database**: H2 (In-Memory) with R2DBC
**선택 이유:**
> 빠른 개발 및 로컬 개발 편의성을 위해 인메모리 기반인 H2 데이터베이스를 사용했습니다.

#### **Cache**: Redis with Lettuce
**선택 이유:**
> 일반적인 DB 트랜잭션 기반 처리 방식은 Race Condition, Lock 경합, 그리고 Connection Pool 한계 등의 이유로 동시 요청 처리 시 중복 신청이나 성능 저하가 발생할 수 있습니다.
> 따라서 선착순 처리는 동시 요청과 중복 신청을 방지, 순서 지정에 대한 포인트가 정확하게 지급되어야 하기 때문에 Redis를 사용하여 원자적이고 빠른 처리를 구현했습니다. 
> Redis는 높은 동시성을 지원하며, Lua 스크립트를 통해 원자적 연산을 보장합니다. 원자적 처리를 위해 Rua script 를 활용했습니다.


### Testing
- RestAssured: REST API 테스트를 활용하여 통합테스트 진행했습니다.
- 동시 10000명 접속 시나리오를 구현하여 성능 테스트를 진행했습니다.
- 동시 10001명 접속 시 시나리오를 구현하여 중복 신청 및 마감 처리 테스트를 진행했습니다.

### 아키텍처 결정 과정

#### **복잡성 vs 확장성**
- **고민**: "단순한 동기식 처리 vs 복잡한 리액티브 처리"
- **결정**: **미래 확장성**을 고려한 리액티브 아키텍처
- **결과**: **Scale-Out 용이성** 확보
> 헥사고날 아키텍처를 사용하여 확장성을 고려했습니다. 저는 미래에 계속 선착순 포인트 지급 시스템이 확장될 여지가 충분한 기능이라고 
> 생각했기 때문에 헥사고날 아키텍처를 선택했습니다. 이로 인해 시스템의 각 구성 요소가 독립적으로 개발, 테스트, 배포될 수 있어 확장성과 유지보수성이 크게 향상되었습니다.

## 🏗️ 프로젝트 구조

```
src/
├── main/kotlin/com/example/wehomework/
│   ├── adapter/
│   │   ├── in/web/           # REST Controllers
│   │   └── out/persistent/  # Repository Implementations, persistentAdapters
│   ├── application/
│   │   ├── port/           # Use Case Interfaces, ports
│   │   └── service/        
│   ├── config/           
│   └── domain/           
└── test/kotlin/com/example/wehomework/
    ├── IntegrationConcurrencyTest/
```

## 🚀 설치 및 실행

### 사전 요구사항
- Java 21+
- Redis Server
- Docker (선택사항)

### Docker 환경 실행
```bash
# app 시작
    docker compose up -d
    
# 실행 상태 확인
    docker compose ps

# 로그 확인
    docker compose logs -f 
```

### Redis CLI 설치 및 실행
``` bash 
docker-compose exec redis redis-cli
```

## 📡 API 명세

### 포인트 신청
```http
POST /api/v1/point/apply
Content-Type: application/json

{
  "userId": 12345
}
```

### 응답 형식

#### ✅ 성공 응답 200 OK
```json
{
  "success": true,
  "message": "포인트 신청이 완료되었습니다.",
  "data": {
    "userId": 12345,
    "order": 1,
    "amount": 100000,
    "createdAt": "2024-07-08T10:30:00"
  }
}
```

#### ❌ 실패 응답
- 409 CONFILT
```json
{
  "success": false,
  "message": "이미 신청한 사용자입니다. (userId: 12345)",
  "errorCode": "DUPLICATE_USER",
  "data": null
}
```

- 400 BAD_REQUEST
```json
{
  "success": false,
  "message": "신청 마감되었습니다.",
  "errorCode": "APPLICATION_CLOSED", 
  "data": null
}
```

- 500 INTERNAL_SERVER_ERROR
```json
{
  "success": false,
  "message": "서버 내부 오류가 발생했습니다.",
  "errorCode": "INTERNAL_SERVER_ERROR", 
  "data": null
}
```

### 포인트 지급 규칙
| 순서 | 지급 포인트 | 대상자 수 |
|------|------------|-----------|
| 1~100번 | 100,000점 | 100명 |
| 101~2,000번 | 50,000점 | 1,900명 |
| 2,001~5,000번 | 20,000점 | 3,000명 |
| 5,001~10,000번 | 10,000점 | 5,000명 |
| 10,001명~ | 지급 없음 | - |

## ⚙️ 핵심 로직

### 1. 선착순 판별 방식
```lua
-- Redis Lua 스크립트 (원자적 연산)
local userId = ARGV[1]
local maxCount = tonumber(ARGV[2])

-- 중복 신청 체크
if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return -1  -- 이미 신청함
end

-- 현재 순서 확인
local currentOrder = tonumber(redis.call('GET', KEYS[1]) or 0)

-- 마감 체크
if currentOrder >= maxCount then
    return -2  -- 마감
end

-- 순서 할당 및 사용자 등록
local newOrder = redis.call('INCR', KEYS[1])
redis.call('SADD', KEYS[2], userId)

return newOrder
```

### 2. 동시성 제어
- **Redis Lua 스크립트**: 원자적 연산으로 race condition 방지
- **Set 자료구조**: O(1) 중복 검사
- **트랜잭션**: DB 일관성 보장
- **롤백 메커니즘**: Redis-DB 불일치 시 자동 복구

### 테스트 시나리오

#### 📊 동시성 테스트
- **목표**: 10,000명 동시 접속 처리
- **검증 항목**:
  - ✅ 정확히 10,000명만 성공
  - ✅ 중복 신청 방지
  - ✅ 데이터 일관성 유지

### Redis 모니터링
```bash
# Redis 상태 확인 스크립트
./redis-monitor.sh

# 수동 확인
redis-cli
KEYS point:*
GET point:order:counter
SCARD point:applied:users
```

