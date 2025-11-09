#!/bin/bash

# =================================================================
# 변수 설정 (실제 환경에 맞게 수정)
# =================================================================
HOSTNAME="http://localhost:8080"
access_key="password"
USERNAME="my-awesome-service"
LONG_URL="https://github.com/google/gemini-api"
NEW_LONG_URL="https://developers.google.com/gemini"

# ANSI 색상 코드
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

step() {
    echo -e "\n${BLUE}=================================================================${NC}"
    echo -e "${YELLOW}STEP $1: $2${NC}"
    echo -e "${BLUE}=================================================================${NC}"
}

fail_if_http_error() {
    local status=$1
    local message=$2
    if [ "$status" -ne 200 ]; then
        echo -e "${RED}Error: ${message} (Status: $status)${NC}"
        exit 1
    fi
}

# =================================================================
# 1. 최초 사용자 등록
# =================================================================
step 1 "최초 사용자 등록"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/auth/register" \
    -H "Content-Type: application/json" \
    -H "X-CLIENTACCESS-KEY: ${access_key}" \
    -d "{\"username\": \"${USERNAME}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"
fail_if_http_error "$HTTP_STATUS" "사용자 등록에 실패했습니다."
echo -e "${GREEN}성공! 사용자 등록 완료${NC}"

# =================================================================
# 2. 등록된 사용자 토큰 발급
# =================================================================
step 2 "등록된 사용자 토큰 발급"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/auth/token/issue" \
    -H "Content-Type: application/json" \
    -H "X-CLIENTACCESS-KEY: ${access_key}" \
    -d "{\"username\": \"${USERNAME}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"
fail_if_http_error "$HTTP_STATUS" "토큰 발급에 실패했습니다."

ACCESS_TOKEN=$(echo "$BODY" | jq -r '.data.token')
REFRESH_TOKEN=$(echo "$BODY" | jq -r '.data.refreshToken')
echo -e "${GREEN}성공! Access Token 발급: ${ACCESS_TOKEN}${NC}"
echo -e "${GREEN}성공! Refresh Token 발급: ${REFRESH_TOKEN}${NC}"

# =================================================================
# 3. Access Token으로 단축 URL 생성
# =================================================================
step 3 "Access Token으로 단축 URL 생성"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/short-url" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -d "{\"longUrl\": \"${LONG_URL}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"
fail_if_http_error "$HTTP_STATUS" "단축 URL 생성에 실패했습니다."

SHORT_URL=$(echo "$BODY" | jq -r '.data.shortUrl')
SHORT_URL_ID=$(echo "$BODY" | jq -r '.data.id')
SHORT_URL_KEY=$(basename "$SHORT_URL")
echo -e "${GREEN}성공! 생성된 단축 URL: ${SHORT_URL} (ID: ${SHORT_URL_ID}, Key: ${SHORT_URL_KEY})${NC}"

# =================================================================
# 4. 생성된 단축 URL로 리디렉션 테스트
# =================================================================
step 4 "생성된 단축 URL로 리디렉션 테스트 (인증 불필요)"
curl -s -I "${HOSTNAME}/r/${SHORT_URL_KEY}" \
    -H "Referer: https://www.google.com/" \
    -H "User-Agent: Gemini-Test-Agent/1.0"
echo -e "${GREEN}리디렉션 요청 완료 (통계 데이터 적재됨)${NC}"

# =================================================================
# 5. Refresh Token으로 토큰 재발급
# =================================================================
step 5 "Refresh Token으로 토큰 재발급"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/auth/token/re-issue" \
    -H "Content-Type: application/json" \
    -H "X-CLIENTACCESS-KEY: ${access_key}" \
    -d "{\"username\": \"${USERNAME}\", \"refreshToken\": \"${REFRESH_TOKEN}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"
fail_if_http_error "$HTTP_STATUS" "토큰 재발급에 실패했습니다."

ACCESS_TOKEN=$(echo "$BODY" | jq -r '.data.token')
REFRESH_TOKEN=$(echo "$BODY" | jq -r '.data.refreshToken')
echo -e "${GREEN}성공! 재발급된 Access Token: ${ACCESS_TOKEN}${NC}"
echo -e "${GREEN}성공! 재발급된 Refresh Token: ${REFRESH_TOKEN}${NC}"

# =================================================================
# 6. 재발급 토큰으로 새로운 단축 URL 생성
# =================================================================
step 6 "재발급 토큰으로 새로운 단축 URL 생성"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/short-url" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -d "{\"longUrl\": \"${NEW_LONG_URL}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"
fail_if_http_error "$HTTP_STATUS" "새로운 단축 URL 생성에 실패했습니다."

SHORT_URL_NEW=$(echo "$BODY" | jq -r '.data.shortUrl')
SHORT_URL_ID_NEW=$(echo "$BODY" | jq -r '.data.id')
SHORT_URL_KEY_NEW=$(basename "$SHORT_URL_NEW")
echo -e "${GREEN}성공! 새 단축 URL: ${SHORT_URL_NEW} (ID: ${SHORT_URL_ID_NEW}, Key: ${SHORT_URL_KEY_NEW})${NC}"

# =================================================================
# 7. 새 단축 URL 리디렉션 및 통계 조회
# =================================================================
step 7 "새 단축 URL 리디렉션 및 통계 조회"
curl -s -I "${HOSTNAME}/r/${SHORT_URL_KEY_NEW}" \
    -H "Referer: https://www.google.com/" \
    -H "User-Agent: Gemini-Test-Agent/1.0"

echo -e "\n${YELLOW}리디렉션 횟수 조회${NC}"
curl -s "${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/count" \
    -H "Accept: application/json"
echo ""

echo -e "\n${YELLOW}통계 조회 (USER_AGENT, MONTH, DAY)${NC}"
curl -s -X POST "${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/stats" \
    -H "Content-Type: application/json" \
    -d '{"groupBy": ["USER_AGENT", "MONTH", "DAY"]}'
echo ""

echo -e "\n${GREEN}모든 API 테스트가 성공적으로 완료되었습니다.${NC}\n"
