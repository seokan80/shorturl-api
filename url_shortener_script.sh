#!/bin/bash

# --- 변수 설정 (실제 환경에 맞게 수정) ---
HOSTNAME="http://localhost:8080"
REGISTRATION_KEY="password"
USERNAME="my-awesome-service4"
LONG_URL="https://github.com/google/gemini-api"

echo "### 1. 최초 사용자 등록 및 API Key 발급 ###"
# API Key를 발급받아 변수에 저장
INITIAL_API_KEY=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-REGISTRATION-KEY: $REGISTRATION_KEY" \
  -d "{\"username\": \"$USERNAME\"}" \
  "$HOSTNAME/api/auth/register" | jq -r '.apiKey')

if [ -z "$INITIAL_API_KEY" ] || [ "$INITIAL_API_KEY" == "null" ]; then
    echo "API Key 발급에 실패했습니다."
    exit 1
fi
echo "발급된 첫 API Key: $INITIAL_API_KEY"
echo ""


echo "### 2. 발급받은 API Key로 단축 URL 생성 ###"
# 단축 URL을 생성하고 응답에서 shortUrl 키를 추출
SHORT_URL_KEY=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $INITIAL_API_KEY" \
  -d "{\"longUrl\": \"$LONG_URL\"}" \
  "$HOSTNAME/api/short-url" | jq -r '.shortUrl' | awk -F/ '{print $NF}')

echo "생성된 단축 URL 키: $SHORT_URL_KEY"
echo ""


echo "### 3. 생성된 단축 URL로 리디렉션 테스트 ###"
# -v 옵션으로 헤더를 확인하여 'Location' 헤더를 통해 리디렉션을 확인
curl -v GET "$HOSTNAME/r/$SHORT_URL_KEY"
echo ""
echo ""


echo "### 4. API Key 만료 시나리오: 토큰 재발급 요청 ###"
# 만료된(기존) 토큰을 사용하여 새 토큰 재발급
REISSUED_API_KEY=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-REGISTRATION-KEY: $REGISTRATION_KEY" \
  -d "{\"username\": \"$USERNAME\", \"apiKey\": \"$INITIAL_API_KEY\"}" \
  "$HOSTNAME/api/auth/token" | jq -r '.token')

if [ -z "$REISSUED_API_KEY" ] || [ "$REISSUED_API_KEY" == "null" ]; then
    echo "API Key 재발급에 실패했습니다."
    exit 1
fi
echo "재발급된 API Key: $REISSUED_API_KEY"
echo ""


echo "### 5. 새로 발급받은 API Key로 다른 단축 URL 생성 테스트 ###"
# 재발급받은 키로 API가 정상 동작하는지 확인
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $REISSUED_API_KEY" \
  -d '{"longUrl": "https://developers.google.com/gemini"}' \
  "$HOSTNAME/api/short-url" | jq
echo ""
echo "모든 과정이 완료되었습니다."
