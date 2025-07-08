#!/bin/bash

echo "🔍 Redis 포인트 시스템 모니터링"
echo "=================================="

# Redis 접속 확인
if ! redis-cli ping > /dev/null 2>&1; then
    echo "❌ Redis 서버에 연결할 수 없습니다."
    exit 1
fi

while true; do
    clear
    echo "🔍 Redis 포인트 시스템 모니터링 - $(date)"
    echo "=================================="
    
    # 현재 신청 순서
    ORDER=$(redis-cli GET point:order:counter 2>/dev/null || echo "0")
    echo "📊 현재 신청 순서: $ORDER"
    
    # 신청한 사용자 수
    USER_COUNT=$(redis-cli SCARD point:applied:users 2>/dev/null || echo "0")
    echo "👥 신청한 사용자 수: $USER_COUNT"
    
    # 남은 자리
    REMAINING=$((10000 - ORDER))
    echo "🎯 남은 자리: $REMAINING"
    
    # 최근 신청한 사용자들 (최대 10명)
    echo "📝 최근 신청한 사용자들:"
    redis-cli SRANDMEMBER point:applied:users 10 2>/dev/null | head -10 | sed 's/^/  - /'
    
    echo ""
    echo "새로고침: 3초마다 | 종료: Ctrl+C"
    echo "=================================="
    
    sleep 3
done
