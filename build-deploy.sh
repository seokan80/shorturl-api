#!/usr/bin/env bash
# ============================================================
# short-url 배포용 WAR 빌드 스크립트
#
# 사용법:
#   ./build-deploy.sh
#
# 산출물:
#   short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.war   (admin + UI)
#   short-url-redirect/build/libs/short-url-redirect-0.0.1-SNAPSHOT.war (redirect)
#
# 프로파일은 빌드 시 결정되지 않고 Tomcat 기동 시 지정한다:
#   export JAVA_OPTS="-Dspring.profiles.active=dev"
# ============================================================
set -euo pipefail

ADMIN_WAR="short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.war"
REDIRECT_WAR="short-url-redirect/build/libs/short-url-redirect-0.0.1-SNAPSHOT.war"

echo "========================================"
echo " short-url 전체 WAR 빌드"
echo "========================================"

./gradlew clean \
  :short-url-admin:bootWar \
  :short-url-redirect:bootWar \
  -x test \
  --no-daemon \
  --stacktrace

echo ""
echo "========================================"
echo " 빌드 완료"
echo "========================================"

check_war() {
  local path="$1" label="$2" ui_check="$3"
  if [ ! -f "${path}" ]; then
    echo "[ERROR] ${label} WAR 없음: ${path}"
    exit 1
  fi
  local size
  size=$(du -sh "${path}" | cut -f1)
  echo "  ${label}"
  echo "    파일  : ${path}"
  echo "    크기  : ${size}"
  if [ "${ui_check}" = "true" ]; then
    local cnt
    cnt=$(unzip -l "${path}" 2>/dev/null | grep -c "WEB-INF/classes/static/assets/" || true)
    if [ "${cnt}" -gt 0 ]; then
      echo "    UI    : 정적 자산 ${cnt}개 포함 ✓"
    else
      echo "    UI    : [WARNING] 정적 자산 없음"
    fi
  fi
  echo ""
}

check_war "${ADMIN_WAR}"    "short-url-admin   (관리 UI + CRUD API)" "true"
check_war "${REDIRECT_WAR}" "short-url-redirect (리다이렉트 서버)"    "false"

echo "배포 방법:"
echo "  # admin (컨텍스트 패스: /s)"
echo "  cp ${ADMIN_WAR} \$CATALINA_HOME/webapps/s.war"
echo ""
echo "  # redirect (컨텍스트 패스: /r 또는 ROOT)"
echo "  cp ${REDIRECT_WAR} \$CATALINA_HOME/webapps/r.war"
echo "  # 루트로 서비스할 경우: ROOT.war"
echo ""
echo "프로파일 지정 (Tomcat setenv.sh):"
echo "  export JAVA_OPTS=\"-Dspring.profiles.active=dev\""
echo ""
echo "배포 검증:"
echo "  curl -I http://host/r/verify   # redirect 스모크 테스트"
echo "  curl    http://host/s/         # admin UI"
