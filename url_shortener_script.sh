#!/bin/bash

# =================================================================
# 변수 설정 (실제 환경에 맞게 수정)
# =================================================================
HOSTNAME="http://localhost:8080"
REGISTRATION_KEY="password"
USERNAME="my-awesome-service"
LONG_URL="https://github.com/google/gemini-api"
NEW_LONG_URL="https://developers.google.com/gemini"

# ANSI 색상 코드
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 각 단계를 구분하는 함수
step() {
    echo -e "\n${BLUE}=================================================================${NC}"
    echo -e "${YELLOW}STEP $1: $2${NC}"
    echo -e "${BLUE}=================================================================${NC}"
}

# =================================================================
# 1. 최초 사용자 등록 및 API Key(Access Token) 발급
# =================================================================
step 1 "최초 사용자 등록 및 API Key(Access Token) 발급"

# -s: silent mode, -w "%{http_code}": write out http status code
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/auth/register" \
-H "Content-Type: application/json" \
-H "X-REGISTRATION-KEY: ${REGISTRATION_KEY}" \
-d "{\"username\": \"${USERNAME}\"}")

# 마지막 줄에서 HTTP 상태 코드 추출
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
# 마지막 줄을 제외한 나머지 내용(body) 추출
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo -e "${RED}Error: API Key 발급에 실패했습니다. (Status: $HTTP_STATUS)${NC}"
    exit 1
fi

INITIAL_API_KEY=$(echo "$BODY" | jq -r '.data.apiKey')

if [ -z "$INITIAL_API_KEY" ] || [ "$INITIAL_API_KEY" == "null" ]; then
    echo -e "${RED}Error: 응답에서 apiKey를 찾을 수 없습니다.${NC}"
    exit 1
fi
echo -e "${GREEN}성공! 발급된 Initial API Key: ${INITIAL_API_KEY}${NC}"


# =================================================================
# 2. 발급받은 API Key로 단축 URL 생성
# =================================================================
step 2 "발급받은 API Key로 단축 URL 생성"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/short-url" \
-H "Content-Type: application/json" \
-H "Authorization: Bearer ${INITIAL_API_KEY}" \
-d "{\"longUrl\": \"${LONG_URL}\", \"username\": \"${USERNAME}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo -e "${RED}Error: 단축 URL 생성에 실패했습니다. (Status: $HTTP_STATUS)${NC}"
    exit 1
fi

SHORT_URL=$(echo "$BODY" | jq -r '.data.shortUrl')
SHORT_URL_ID=$(echo "$BODY" | jq -r '.data.id')
# URL에서 마지막 '/' 이후의 문자열(shortUrlKey) 추출
SHORT_URL_KEY=$(basename "$SHORT_URL")

echo -e "${GREEN}성공! 생성된 단축 URL: ${SHORT_URL} (ID: ${SHORT_URL_ID}, Key: ${SHORT_URL_KEY})${NC}"


# =================================================================
# 3. 생성된 단축 URL로 리디렉션 테스트
# =================================================================
step 3 "생성된 단축 URL로 리디렉션 테스트 (인증 불필요)"
echo "Requesting: ${HOSTNAME}/r/${SHORT_URL_KEY}"
# -I 옵션으로 헤더 정보만 확인하여 리디렉션 확인
curl -s -I "${HOSTNAME}/r/${SHORT_URL_KEY}" \
-H "Referer: https://www.google.com/" \
-H "User-Agent: Gemini-Test-Agent/1.0"

echo -e "${GREEN}리디렉션 요청 완료 (통계 데이터 적재됨)${NC}"


# =================================================================
# 4. API Key 만료 시나리오: 토큰 재발급 요청
# =================================================================
step 4 "API Key 만료 시나리오: 토큰 재발급 요청"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/auth/token" \
-H "Content-Type: application/json" \
-H "X-REGISTRATION-KEY: ${REGISTRATION_KEY}" \
-d "{\"username\": \"${USERNAME}\", \"apiKey\": \"${INITIAL_API_KEY}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo -e "${RED}Error: 토큰 재발급에 실패했습니다. (Status: $HTTP_STATUS)${NC}"
    exit 1
fi

REISSUED_API_KEY=$(echo "$BODY" | jq -r '.data.token')
echo -e "${GREEN}성공! 재발급된 API Key: ${REISSUED_API_KEY}${NC}"


# =================================================================
# 5. 새로 발급받은 API Key로 다른 단축 URL 생성 테스트
# =================================================================
step 5 "새로 발급받은 API Key로 다른 단축 URL 생성 테스트"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${HOSTNAME}/api/short-url" \
-H "Content-Type: application/json" \
-H "Authorization: Bearer ${REISSUED_API_KEY}" \
-d "{\"longUrl\": \"${NEW_LONG_URL}\", \"username\": \"${USERNAME}\"}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response Body: ${BODY}"
echo "HTTP Status: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo -e "${RED}Error: 새로운 단축 URL 생성에 실패했습니다. (Status: $HTTP_STATUS)${NC}"
    exit 1
fi

# 새로 생성된 URL 정보를 위해 변수 업데이트
SHORT_URL_NEW=$(echo "$BODY" | jq -r '.data.shortUrl')
SHORT_URL_ID_NEW=$(echo "$BODY" | jq -r '.data.id')
SHORT_URL_KEY_NEW=$(basename "$SHORT_URL_NEW")

echo -e "${GREEN}성공! 새로 생성된 단축 URL: ${SHORT_URL_NEW} (ID: ${SHORT_URL_ID_NEW}, Key: ${SHORT_URL_KEY_NEW})${NC}"


# =================================================================
# 6. 새로 생성된 단축 URL로 리디렉션 테스트
# =================================================================
step 6 "새로 생성된 단축 URL로 리디렉션 테스트"
echo "Requesting: ${HOSTNAME}/r/${SHORT_URL_KEY_NEW}"
curl -s -I "${HOSTNAME}/r/${SHORT_URL_KEY_NEW}" \
-H "Referer: https://www.google.com/" \
-H "User-Agent: Gemini-Test-Agent/1.0"

echo -e "${GREEN}리디렉션 요청 완료 (통계 데이터 적재됨)${NC}"


# =================================================================
# 7. 단축 URL 리디렉션 횟수 조회 (새로운 URL 기준)
# =================================================================
step 7 "단축 URL 리디렉션 횟수 조회 (새로운 URL 기준)"
echo "Requesting: ${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/count"
curl -s "${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/count" \
-H "Accept: application/json"
echo "" # for new line


# =================================================================
# 8. 단축 URL 통계 조회 (Referer, Year 기준) (새로운 URL 기준)
# =================================================================
step 8 "단축 URL 통계 조회 (Referer, Year 기준)"
echo "Requesting: ${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/stats"
curl -s -X POST "${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/stats" \
-H "Content-Type: application/json" \
-d '{"groupBy": ["REFERER", "YEAR"]}'
echo "" # for new line


# =================================================================
# 9. 단축 URL 통계 조회 (User-Agent, Month, Day 기준) (새로운 URL 기준)
# =================================================================
step 9 "단축 URL 통계 조회 (User-Agent, Month, Day 기준)"
echo "Requesting: ${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/stats"
curl -s -X POST "${HOSTNAME}/r/history/${SHORT_URL_ID_NEW}/stats" \
-H "Content-Type: application/json" \
-d '{"groupBy": ["USER_AGENT", "MONTH", "DAY"]}'
echo "" # for new line

echo -e "\n${GREEN}모든 API 테스트가 성공적으로 완료되었습니다.${NC}\n"
