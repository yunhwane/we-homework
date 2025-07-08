#!/bin/bash

echo "🚀 포인트 시스템 부하 테스트 스크립트"
echo "=================================="

# 서버가 실행 중인지 확인
check_server() {
    echo "📡 서버 상태 확인 중..."
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/point/current-order || echo "000")
    
    if [ "$response" != "200" ]; then
        echo "❌ 서버가 실행되지 않았습니다. 먼저 애플리케이션을 실행해주세요:"
        echo "   ./gradlew bootRun"
        exit 1
    fi
    echo "✅ 서버가 실행 중입니다."
}

# Redis 상태 확인
check_redis() {
    echo "📡 Redis 상태 확인 중..."
    if ! command -v redis-cli &> /dev/null; then
        echo "⚠️  redis-cli가 설치되지 않았습니다. Redis 상태를 확인할 수 없습니다."
        return
    fi
    
    if redis-cli ping | grep -q PONG; then
        echo "✅ Redis가 실행 중입니다."
    else
        echo "❌ Redis가 실행되지 않았습니다. Redis를 먼저 실행해주세요:"
        echo "   redis-server"
        echo "   또는 Docker: docker run -d -p 6379:6379 redis"
        exit 1
    fi
}

# 동시성 테스트 함수
run_concurrent_test() {
    local user_count=$1
    local description=$2
    
    echo ""
    echo "🎯 $description ($user_count명)"
    echo "----------------------------------------"
    
    # 임시 파일 생성
    temp_file=$(mktemp)
    
    # 병렬로 요청 전송
    seq 1 $user_count | xargs -I {} -P 100 bash -c '
        user_id=$((RANDOM * RANDOM + {}))
        start_time=$(date +%s%3N)
        
        response=$(curl -s -w "\n%{http_code}" \
            -X POST \
            -H "Content-Type: application/json" \
            -d "{\"userId\": $user_id}" \
            http://localhost:8080/api/v1/point/apply)
        
        http_code=$(echo "$response" | tail -n1)
        body=$(echo "$response" | head -n-1)
        end_time=$(date +%s%3N)
        duration=$((end_time - start_time))
        
        if [ "$http_code" = "200" ]; then
            if echo "$body" | grep -q "\"success\":true"; then
                echo "SUCCESS,$user_id,$duration" >> '"$temp_file"'
            else
                echo "BUSINESS_ERROR,$user_id,$duration" >> '"$temp_file"'
            fi
        else
            echo "HTTP_ERROR,$user_id,$duration,$http_code" >> '"$temp_file"'
        fi
    '
    
    # 결과 분석
    if [ -f "$temp_file" ]; then
        total_requests=$user_count
        success_count=$(grep -c "^SUCCESS" "$temp_file" 2>/dev/null || echo 0)
        business_error_count=$(grep -c "^BUSINESS_ERROR" "$temp_file" 2>/dev/null || echo 0)
        http_error_count=$(grep -c "^HTTP_ERROR" "$temp_file" 2>/dev/null || echo 0)
        
        echo "📊 결과:"
        echo "  총 요청: $total_requests"
        echo "  성공: $success_count"
        echo "  비즈니스 에러: $business_error_count"
        echo "  HTTP 에러: $http_error_count"
        echo "  성공률: $(echo "scale=2; $success_count * 100 / $total_requests" | bc -l)%"
        
        # 응답 시간 분석
        if [ $success_count -gt 0 ]; then
            avg_time=$(grep "^SUCCESS" "$temp_file" | cut -d',' -f3 | awk '{sum+=$1} END {print sum/NR}' 2>/dev/null || echo 0)
            echo "  평균 응답시간: ${avg_time}ms"
        fi
        
        rm -f "$temp_file"
    fi
}

# 순차 테스트 함수
run_sequential_test() {
    echo ""
    echo "🎯 순차 테스트 (10,001명 - 마감 확인)"
    echo "----------------------------------------"
    
    success_count=0
    closed_count=0
    
    for i in {1..10001}; do
        user_id=$((RANDOM * RANDOM + i))
        
        response=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "{\"userId\": $user_id}" \
            http://localhost:8080/api/v1/point/apply)
        
        if echo "$response" | grep -q "\"success\":true"; then
            success_count=$((success_count + 1))
        elif echo "$response" | grep -q "마감"; then
            closed_count=$((closed_count + 1))
        fi
        
        # 진행상황 출력
        if [ $((i % 1000)) -eq 0 ]; then
            echo "  진행: $i/10001 (성공: $success_count, 마감: $closed_count)"
        fi
        
        # 마감 메시지가 나오면 중단
        if [ $closed_count -gt 0 ] && [ $success_count -ge 10000 ]; then
            echo "  ✅ 예상대로 10,000명 이후 마감되었습니다."
            break
        fi
    done
    
    echo "📊 순차 테스트 결과:"
    echo "  성공: $success_count"
    echo "  마감: $closed_count"
}

# 메인 실행부
main() {
    check_server
    check_redis
    
    echo ""
    echo "🧪 테스트 시작!"
    echo "=================================="
    
    # 다양한 부하로 테스트
    run_concurrent_test 100 "웜업 테스트"
    sleep 2
    
    run_concurrent_test 1000 "중간 부하 테스트"
    sleep 3
    
    run_concurrent_test 5000 "높은 부하 테스트"
    sleep 3
    
    run_concurrent_test 10000 "최대 부하 테스트"
    
    # 순차 테스트로 마감 확인
    run_sequential_test
    
    echo ""
    echo "🎉 모든 테스트 완료!"
}

# bc가 설치되어 있는지 확인
if ! command -v bc &> /dev/null; then
    echo "⚠️  bc가 설치되지 않았습니다. 계산 기능이 제한됩니다."
    echo "   설치: sudo apt-get install bc (Ubuntu) 또는 brew install bc (macOS)"
fi

# 스크립트 실행
main "$@"
