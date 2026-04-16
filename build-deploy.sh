#!/usr/bin/env bash
# ============================================================
# short-url 배포용 빌드 스크립트
#
# 산출물:
#   short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar   (JAR, 내장 Tomcat)
#   short-url-redirect/build/libs/short-url-redirect-0.0.1-SNAPSHOT.war (WAR, 외장 Tomcat)
#
# 프로파일은 실행 시 지정한다:
#   [admin JAR]    java -jar short-url-admin-*.jar --spring.profiles.active=dev
#   [redirect WAR] JAVA_OPTS="-Dspring.profiles.active=dev"  (Tomcat setenv.sh)
# ============================================================
set -euo pipefail

ADMIN_JAR="short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar"
REDIRECT_WAR="short-url-redirect/build/libs/short-url-redirect-0.0.1-SNAPSHOT.war"

echo "========================================"
echo " short-url 전체 빌드"
echo "  admin    → JAR (내장 Tomcat)"
echo "  redirect → WAR (외장 Tomcat)"
echo "========================================"

./gradlew clean \
  :short-url-admin:bootJar \
  :short-url-redirect:bootWar \
  -x test \
  --no-daemon \
  --stacktrace

echo ""
echo "========================================"
echo " 빌드 완료"
echo "========================================"

check_artifact() {
  local path="$1" label="$2" type="$3"
  if [ ! -f "${path}" ]; then
    echo "[ERROR] ${label} 산출물 없음: ${path}"
    exit 1
  fi
  local size
  size=$(du -sh "${path}" | cut -f1)
  echo "  ${label} (${type})"
  echo "    파일 : ${path}"
  echo "    크기 : ${size}"

  if [[ "${type}" == "JAR" ]]; then
    local cnt
    cnt=$(unzip -l "${path}" 2>/dev/null | grep -c "BOOT-INF/classes/static/assets/" || true)
    if [ "${cnt}" -gt 0 ]; then
      echo "    UI   : 정적 자산 ${cnt}개 포함 ✓"
    else
      echo "    UI   : [WARNING] 정적 자산 없음"
    fi
  fi
  echo ""
}

check_artifact "${ADMIN_JAR}"    "short-url-admin   (관리 UI + CRUD API)" "JAR"
check_artifact "${REDIRECT_WAR}" "short-url-redirect (리다이렉트 서버)"   "WAR"

echo "배포 방법:"
echo ""
echo "  # admin — JAR 직접 실행"
echo "  java -jar ${ADMIN_JAR} --spring.profiles.active=dev"
echo ""
echo "  # redirect — Tomcat webapps 에 복사 (컨텍스트 패스 /r 또는 ROOT)"
echo "  cp ${REDIRECT_WAR} \$CATALINA_HOME/webapps/r.war"
echo "  # 루트로 서비스할 경우: ROOT.war"
echo ""
echo "  # Tomcat 프로파일 지정 (setenv.sh):"
echo "  export JAVA_OPTS=\"-Dspring.profiles.active=dev\""
echo ""
echo "배포 검증:"
echo "  curl -I http://host:8081/r/verify   # redirect 스모크 테스트"
echo "  curl    http://host:8080/           # admin UI"
