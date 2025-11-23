#!/bin/bash

# deploy.sh - Blue-Green 무중단 배포 스크립트

set -e  # 에러 발생 시 스크립트 중단

echo "=== Blue-Green 배포 시작 ==="

echo "이미지 업데이트 중..."
if ! sudo docker compose pull; then
    echo "❌ Docker 이미지 pull 실패! GitHub Actions 빌드를 확인해주세요."
    echo "❌ 배포를 중단합니다."
    exit 1
fi
echo "✅ 새로운 이미지가 성공적으로 pull되었습니다."

echo "사용하지 않는 이미지 정리 중..."
sudo docker image prune -f

# 현재 활성화된 컨테이너 확인
EXIST_BLUE=$(sudo docker ps --filter "name=blue" --filter "status=running" -q)

if [ -z "$EXIST_BLUE" ]; then
    echo "BLUE 컨테이너 실행"
    sudo docker compose up -d blue
    BEFORE_COLOR="green"
    AFTER_COLOR="blue"
    BEFORE_PORT=8081
    AFTER_PORT=8080
else
    echo "GREEN 컨테이너 실행"
    sudo docker compose up -d green
    BEFORE_COLOR="blue"
    AFTER_COLOR="green"
    BEFORE_PORT=8080
    AFTER_PORT=8081
fi

echo "${AFTER_COLOR} server up (port:${AFTER_PORT})"

# 서버 응답 확인 (헬스체크)
echo "서버 헬스체크 시작..."
HEALTH_CHECK_COUNT=0
MAX_RETRY=30  # 최대 5분 대기 (10초 * 30회)

HEALTH_URL="http://127.0.0.1:${AFTER_PORT}/health-check"

while [ $HEALTH_CHECK_COUNT -lt $MAX_RETRY ]; do
    HEALTH_CHECK_COUNT=$((HEALTH_CHECK_COUNT + 1))
    echo "서버 응답 확인중 (${HEALTH_CHECK_COUNT}/${MAX_RETRY})"

    # curl로 헬스체크 (타임아웃 5초, 실패 시 failed 문자열로 치환)
    UP=$(curl -s --connect-timeout 5 --max-time 10 "${HEALTH_URL}" 2>/dev/null || echo "failed")

    if echo "${UP}" | grep -q "OK"; then
        echo "✅ 서버가 정상적으로 구동되었습니다!"
        break
    else
        echo "⏳ 서버 응답 대기 중... (${UP})"
        sleep 10
    fi
done

# 헬스체크 실패 시 롤백
if [ $HEALTH_CHECK_COUNT -eq $MAX_RETRY ]; then
    echo "❌ 서버가 정상적으로 구동되지 않았습니다. 롤백을 시작합니다."
    sudo docker compose stop ${AFTER_COLOR}
    sudo docker compose rm -f ${AFTER_COLOR}

    # 이전 컨테이너가 있다면 다시 시작
    if [ "${BEFORE_COLOR}" != "" ]; then
        echo "이전 ${BEFORE_COLOR} 컨테이너를 다시 시작합니다."
        sudo docker compose up -d ${BEFORE_COLOR}
    fi

    echo "❌ 배포 실패 - 롤백 완료"
    exit 1
fi

# Nginx 설정 파일 백업
if [ -f /etc/nginx/conf.d/service-url.inc ]; then
    sudo cp /etc/nginx/conf.d/service-url.inc /etc/nginx/conf.d/service-url.inc.backup
fi

# Nginx 설정 업데이트
echo "Nginx 설정 업데이트 중..."
if [ -f /etc/nginx/conf.d/service-url.inc ]; then
    sudo sed -i "s/${BEFORE_PORT}/${AFTER_PORT}/g" /etc/nginx/conf.d/service-url.inc

    # Nginx 설정 문법 검사
    if sudo nginx -t; then
        sudo nginx -s reload
        echo "✅ Nginx 설정이 성공적으로 업데이트되었습니다."
    else
        echo "❌ Nginx 설정 오류. 백업 파일로 복원합니다."
        sudo cp /etc/nginx/conf.d/service-url.inc.backup /etc/nginx/conf.d/service-url.inc
        sudo nginx -s reload
        exit 1
    fi
else
    echo "⚠️  Nginx 설정 파일을 찾을 수 없습니다: /etc/nginx/conf.d/service-url.inc"
fi

# 트래픽 전환 확인 (Nginx 기준 헬스체크)
echo "트래픽 전환 확인 중..."
sleep 5
NGINX_CHECK=$(curl -s --connect-timeout 5 http://127.0.0.1/api/health-check 2>/dev/null || echo "failed")
if echo "${NGINX_CHECK}" | grep -q "OK"; then
    echo "✅ 트래픽이 성공적으로 전환되었습니다."
else
    echo "⚠️ 트래픽 전환 확인 실패. 수동으로 확인해주세요."
fi

# 이전 컨테이너 종료 (안전하게)
if [ "${BEFORE_COLOR}" != "" ]; then
    echo "${BEFORE_COLOR} server down (port:${BEFORE_PORT})"

    # 잠시 대기 후 이전 컨테이너 종료
    echo "이전 컨테이너 종료 전 30초 대기..."
    sleep 30

    sudo docker compose stop ${BEFORE_COLOR} 2>/dev/null || true
    sudo docker compose rm -f ${BEFORE_COLOR} 2>/dev/null || true
    echo "✅ 이전 ${BEFORE_COLOR} 컨테이너가 종료되었습니다."
fi

# 배포 완료
echo ""
echo "🎉 Deploy Completed!!"
echo "✅ Active Container: ${AFTER_COLOR} (Port: ${AFTER_PORT})"
echo "✅ Deployment Time: $(date)"
echo ""

# 최종 상태 확인
echo "=== 배포 후 상태 확인 ==="
echo "실행 중인 컨테이너:"
sudo docker ps --filter "name=blue" --filter "name=green" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "=== 배포 완료 ==="
