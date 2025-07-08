#!/bin/bash

echo "📊 포인트 시스템 실시간 모니터링"
echo "================================"
echo "종료하려면 Ctrl+C를 누르세요"
echo ""

# 함수 정의
get_current_order() {
    curl -s http://localhost:8080/api/v1/point/current-order 2>/dev/null | \
    grep -o '"data":[0-9]*' | cut -d':' -f2 || echo "0"
}

get_redis_stats() {
    if command -v redis-cli &> /dev/null; then
        local counter=$(redis-cli get "point:order:counter" 2>/dev/null || echo "0")
        local users=$(redis-cli scard "point:applied:users" 2>/dev/null || echo "0")
        echo "Redis - Counter: $counter, Users: $users"
    else
        echo "Redis - N/A (redis-cli not found)"
    fi
}

monitor_system() {
    local memory_usage=""
    local cpu_usage=""
    
    if command -v free &> /dev/null; then
        memory_usage=$(free -h | awk 'NR==2{printf "%.1f%%", $3*100/$2}')
    fi
    
    if command -v top &> /dev/null; then
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    fi
    
    echo "System - Memory: ${memory_usage:-N/A}, CPU: ${cpu_usage:-N/A}%"
}

test_endpoint() {
    local start_time=$(date +%s%3N)
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "{\"userId\": $(date +%s)}" \
        http://localhost:8080/api/v1/point/apply 2>/dev/null)
    local end_time=$(date +%s%3N)
    local duration=$((end_time - start_time))
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q "\"success\":true"; then
            echo "API Test - ✅ SUCCESS (${duration}ms)"
        else
            echo "API Test - ⚠️  BUSINESS ERROR (${duration}ms)"
        fi
    else
        echo "API Test - ❌ HTTP ERROR $http_code (${duration}ms)"
    fi
}

# 메인 모니터링 루프
counter=0
while true; do
    clear
    echo "📊 포인트 시스템 실시간 모니터링 - $(date '+%Y-%m-%d %H:%M:%S')"
    echo "================================"
    echo ""
    
    # 현재 상태
    echo "🎯 현재 상태:"
    current_order=$(get_current_order)
    echo "  신청 순서: $current_order"
    
    # Redis 상태
    echo ""
    echo "🔴 Redis 상태:"
    get_redis_stats
    
    # 시스템 리소스
    echo ""
    echo "💻 시스템 리소스:"
    monitor_system
    
    # API 테스트 (10초마다)
    if [ $((counter % 2)) -eq 0 ]; then
        echo ""
        echo "🌐 API 상태:"
        test_endpoint
    fi
    
    echo ""
    echo "📈 통계:"
    echo "  총 모니터링 시간: $((counter * 5))초"
    echo "  마지막 업데이트: $(date '+%H:%M:%S')"
    
    if [ "$current_order" -ge 10000 ]; then
        echo ""
        echo "🚫 신청 마감! (10,000명 달성)"
    fi
    
    echo ""
    echo "================================"
    echo "새로고침: 5초마다 | 종료: Ctrl+C"
    
    counter=$((counter + 1))
    sleep 5
done
