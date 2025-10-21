#!/bin/bash

# =================================================================
# 변수 설정
# =================================================================
HOSTNAME="http://localhost:8080"
REGISTRATION_KEY="password"
USERNAME="my-awesome-service-shell-script"
LONG_URL="https://www.google.com"

# jq 설치 확인
if ! command -v jq &> /dev/null
then
    echo "jq could not be found. Please install jq to run this script."
    exit
fi

# =================================================================
# 1. 사용자 등록 및 API Key 발급
# =================================================================
echo "### 1. Registering user and getting API Key..."
API_KEY_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-REGISTRATION-KEY: $REGISTRATION_KEY" \
  -d "{\"username\": \"$USERNAME\"}" \
  "$HOSTNAME/api/auth/register")

API_KEY=$(echo "$API_KEY_RESPONSE" | jq -r '.data.apiKey')

if [ "$API_KEY" == "null" ] || [ -z "$API_KEY" ]; then
    echo "Failed to get API Key. Response: $API_KEY_RESPONSE"
    exit 1
fi
echo "API Key received: $API_KEY"
echo ""


# =================================================================
# 2. 단축 URL 생성
# =================================================================
echo "### 2. Creating a short URL..."
SHORT_URL_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d "{\"longUrl\": \"$LONG_URL\", \"username\": \"$USERNAME\"}" \
  "$HOSTNAME/api/short-url")

# 응답에서 shortUrl 키와 id 추출
SHORT_URL_KEY=$(echo "$SHORT_URL_RESPONSE" | jq -r '.data.shortUrl' | awk -F'/' '{print $NF}')
SHORT_URL_ID=$(echo "$SHORT_URL_RESPONSE" | jq -r '.data.id')


if [ -z "$SHORT_URL_KEY" ] || [ "$SHORT_URL_ID" == "null" ]; then
    echo "Failed to create short URL. Response: $SHORT_URL_RESPONSE"
    exit 1
fi

echo "Short URL Key created: $SHORT_URL_KEY"
echo "Short URL ID created: $SHORT_URL_ID"
echo ""


# =================================================================
# 3. 리디렉션 테스트 (통계 데이터 적재 목적)
# =================================================================
echo "### 3. Testing redirection (this will also create history data)..."
# -A: User-Agent, -e: Referer
curl -s -o /dev/null -w "Redirect 1 -> Status: %{http_code}\n" -L -A "Test-Agent/1.0" -e "https://test.com" "$HOSTNAME/r/$SHORT_URL_KEY"
curl -s -o /dev/null -w "Redirect 2 -> Status: %{http_code}\n" -L -A "Another-Agent/2.0" -e "https://anothertest.com" "$HOSTNAME/r/$SHORT_URL_KEY"
curl -s -o /dev/null -w "Redirect 3 -> Status: %{http_code}\n" -L -A "Test-Agent/1.0" -e "https://test.com" "$HOSTNAME/r/$SHORT_URL_KEY"
echo ""


# =================================================================
# 4. 단축 URL 리디렉션 횟수 조회
# =================================================================
echo "### 4. Getting redirection count..."
COUNT_RESPONSE=$(curl -s -X GET \
  -H "Accept: application/json" \
  "$HOSTNAME/r/history/$SHORT_URL_ID/count")

echo "Count Response: "
echo "$COUNT_RESPONSE" | jq .
echo ""


# =================================================================
# 5. 단축 URL 통계 조회 (Referer, Year 기준)
# =================================================================
echo "### 5. Getting stats by Referer and Year..."
STATS_RESPONSE_1=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"groupBy": ["REFERER", "YEAR"]}' \
  "$HOSTNAME/r/history/$SHORT_URL_ID/stats")

echo "Stats (Referer, Year) Response: "
echo "$STATS_RESPONSE_1" | jq .
echo ""


# =================================================================
# 6. 단축 URL 통계 조회 (User-Agent 기준)
# =================================================================
echo "### 6. Getting stats by User-Agent..."
STATS_RESPONSE_2=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"groupBy": ["USER_AGENT"]}' \
  "$HOSTNAME/r/history/$SHORT_URL_ID/stats")

echo "Stats (User-Agent) Response: "
echo "$STATS_RESPONSE_2" | jq .
echo ""

echo "Script finished."
