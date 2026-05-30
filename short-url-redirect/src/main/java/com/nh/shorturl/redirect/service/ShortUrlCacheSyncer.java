package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 단축 URL 캐시 동기화.
 *
 * <ul>
 *   <li>기동 시: 전체 URL 을 캐시에 로드 (full load)</li>
 *   <li>주기적: 변경분만 증분 반영 (incremental sync)
 *       <ul>
 *         <li>삭제(deleted=true) 또는 만료(expiredAt &lt; now) → 캐시에서 evict</li>
 *         <li>그 외 → 캐시에 put (신규 또는 수정)</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheSyncer implements ApplicationRunner {

    private final WebClient webClient;
    private final ShortUrlCacheService cacheService;

    @Value("${short-url.admin.api.uri.all}")
    private String uriAll;

    @Value("${short-url.admin.api.uri.changes}")
    private String uriChanges;

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();

    @Override
    public void run(ApplicationArguments args) {
        fullLoad();
    }

    /**
     * 주기적으로 변경분을 폴링하여 캐시에 반영한다.
     * 주기는 application.yml 의 short-url.sync.cache-interval-ms 로 설정.
     */
    @Scheduled(fixedRateString = "${short-url.sync.cache-interval-ms:300000}",
               initialDelayString = "${short-url.sync.cache-initial-delay-ms:300000}")
    public void incrementalSync() {
        LocalDateTime since = lastSyncTime.get();
        if (since == null) {
            fullLoad();
            return;
        }

        // fetch 도중 발생한 변경이 누락되지 않도록, 폴링 시작 시각을 다음 since 기준으로 삼는다.
        // (put/evict 는 멱등이므로 경계의 소폭 중복 반영은 무해하다.)
        LocalDateTime syncStart = LocalDateTime.now();
        try {
            String sinceParam = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            log.info("[cache-sync] Incremental sync started. since={}, cacheSize={}", sinceParam, cacheService.size());

            List<ShortUrlResponse> changes = fetchList(uriChanges, sinceParam);

            if (changes == null || changes.isEmpty()) {
                log.info("[cache-sync] no changes since {}", sinceParam);
            } else {
                int put = 0, evict = 0;
                LocalDateTime now = LocalDateTime.now();

                for (ShortUrlResponse item : changes) {
                    if (isDeletedOrExpired(item, now)) {
                        cacheService.evict(item.getShortKey());
                        evict++;
                        log.info("[cache-sync]   evict: [{}] {} (deleted={}, expiredAt={})",
                                item.getId(), item.getShortKey(), item.getDeleted(), item.getExpiredAt());
                    } else {
                        cacheService.put(item);
                        put++;
                        log.info("[cache-sync]   put: [{}] {} → {} (expires: {})",
                                item.getId(), item.getShortKey(), item.getLongUrl(), item.getExpiredAt());
                    }
                }
                log.info("[cache-sync] {} changes applied (put={}, evict={}), cacheSize={}",
                        changes.size(), put, evict, cacheService.size());
            }

            lastSyncTime.set(syncStart);
        } catch (Exception e) {
            log.warn("[cache-sync] incremental sync failed, will retry next cycle. cause: {}", e.getMessage());
        }
    }

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_WAIT_MS = 5_000;

    private void fullLoad() {
        log.info("[cache-sync] Starting full cache load...");
        long waitMs = INITIAL_WAIT_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            LocalDateTime attemptStart = LocalDateTime.now();
            try {
                List<ShortUrlResponse> all = fetchList(uriAll);

                if (all != null && !all.isEmpty()) {
                    all.forEach(item -> {
                        cacheService.put(item);
                        log.debug("[cache-sync]   loaded: {} → {}, expiredAt={}",
                                item.getShortKey(), item.getLongUrl(), item.getExpiredAt());
                    });
                    lastSyncTime.set(attemptStart);
                    log.info("[cache-sync] Full load completed. {} items cached.", all.size());
                    if (log.isInfoEnabled() && !all.isEmpty()) {
                        all.forEach(item -> log.info("[cache-sync]   [{}] {} → {} (expires: {})",
                                item.getId(), item.getShortKey(), item.getLongUrl(), item.getExpiredAt()));
                    }
                    return;
                }
            } catch (Exception e) {
                log.warn("[cache-sync] Full load attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    waitMs *= 2;
                }
            }
        }
        log.error("[cache-sync] Full load failed after {} retries. Cache is empty — "
                + "all requests will fall back until next sync cycle.", MAX_RETRIES);
    }

    /**
     * admin API 호출 후 ResultEntity 래퍼에서 data(List) 를 추출한다.
     * 응답 형태: {"code":"0000","message":"Success","data":[...]}
     */
    private List<ShortUrlResponse> fetchList(String uri, Object... uriVars) {
        ResultEntity<List<ShortUrlResponse>> result = webClient.get()
                .uri(uri, uriVars)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResultEntity<List<ShortUrlResponse>>>() {})
                .block();

        if (result == null || result.getData() == null) {
            return Collections.emptyList();
        }
        return result.getData();
    }

    private boolean isDeletedOrExpired(ShortUrlResponse item, LocalDateTime now) {
        if (Boolean.TRUE.equals(item.getDeleted())) {
            return true;
        }
        if (item.getExpiredAt() == null) return false;
        try {
            // admin 은 expiredAt 을 LocalDateTime.toString()(ISO-8601, 예: 2026-05-30T23:59:59.999)으로 내려준다.
            // AppConfig.ttlNanos 와 동일하게 ISO 파서를 사용해 포맷을 일치시킨다.
            LocalDateTime expiry = LocalDateTime.parse(item.getExpiredAt());
            return expiry.isBefore(now);
        } catch (Exception e) {
            return false;
        }
    }
}
