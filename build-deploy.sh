#!/usr/bin/env bash
# ============================================================
# short-url 배포용 WAR 빌드 스크립트
#
# 사용법:
#   ./build-deploy.sh
#
# 프로파일(local/dev/prod)은 빌드 시 결정되지 않고, Tomcat 기동 시 JVM 옵션으로 지정한다.
#   예) JAVA_OPTS="-Dspring.profiles.active=dev"
# ============================================================
set -euo pipefail

ARTIFACT_DIR="short-url-admin/build/libs"
ARTIFACT="short-url-admin-0.0.1-SNAPSHOT.war"

echo "========================================"
echo " short-url WAR 빌드"
echo "========================================"

# 1. 빌드 (테스트 제외, UI 포함)
./gradlew clean :short-url-admin:bootWar \
  -x test \
  --no-daemon \
  --stacktrace

# 2. 산출물 확인
WAR_PATH="${ARTIFACT_DIR}/${ARTIFACT}"
if [ ! -f "${WAR_PATH}" ]; then
  echo "[ERROR] WAR 파일이 생성되지 않았습니다: ${WAR_PATH}"
  exit 1
fi

WAR_SIZE=$(du -sh "${WAR_PATH}" | cut -f1)
echo ""
echo "========================================"
echo " 빌드 완료"
echo "  파일  : ${WAR_PATH}"
echo "  크기  : ${WAR_SIZE}"
echo "========================================"

# 3. UI 자산 포함 여부 확인
UI_COUNT=$(unzip -l "${WAR_PATH}" 2>/dev/null | grep -c "WEB-INF/classes/static/assets/" || true)
if [ "${UI_COUNT}" -gt 0 ]; then
  echo "  UI    : 정적 자산 ${UI_COUNT}개 포함 ✓"
else
  echo "  UI    : [WARNING] 정적 자산 없음 — 프론트 빌드 확인 필요"
fi

echo ""
echo "배포 방법:"
echo "  cp ${WAR_PATH} \$CATALINA_HOME/webapps/s.war"
echo "  (컨텍스트 패스 /s 로 배포 시 파일명을 s.war 로 설정)"
echo ""
echo "프로파일 지정 (Tomcat setenv.sh):"
echo "  export JAVA_OPTS=\"-Dspring.profiles.active=dev\""
echo ""
echo "배포 검증:"
echo "  curl -I http://localhost:8080/s/verify"
