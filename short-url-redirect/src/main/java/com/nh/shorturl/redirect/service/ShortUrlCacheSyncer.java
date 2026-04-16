package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 단축 URL 캐시 동기화.
 *
 * <ul>
 *   <li>기동 시: admin 의 /api/internal/short-urls/all 로 만료되지 않은 전체 URL 을 캐시에 로드</li>
 *   <li>5분 주기: /api/internal/short-urls/changes?since= 로 변경분만 증분 반영
 *       <ul>
 *         <li>삭제(deleted=true) 또는 만료(expiredAt &lt; now) → 캐시에서 evict</li>
 *         <li>그 외 → 캐시에 put (신규 또는 수정)</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>per-entry expiry(AppConfig) 와 조합되어 만료 시각 도달 시 Caffeine 이 자동 제거하므로,
 * 증분 폴링은 신규 URL 반영과 admin 측 삭제/만료일 변경을 동기화하는 역할에 집중한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheSyncer implements ApplicationRunner {

    private final WebClient webClient;
    private final ShortUrlCacheService cacheService;

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();

    @Override
    public void run(ApplicationArguments args) {
        fullLoad();
    }

    /**
     * 5분마다 변경분을 폴링하여 캐시에 반영한다.
     * admin 이 응답하지 않아도 기존 캐시는 유지되며, 다음 폴링에서 재시도한다.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 300_000) // 5분
    public void incrementalSync() {
        LocalDateTime since = lastSyncTime.get();
        if (since == null) {
            // 전체 로드가 실패했던 경우 → 다시 전체 로드 시도
            fullLoad();
            return;
        }

        try {
            String sinceParam = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            List<ShortUrlResponse> changes = webClient.get()
                    .uri("/api/internal/short-urls/changes?since={since}", sinceParam)
                    .retrieve()
                    .bodyToFlux(ShortUrlResponse.class)
                    .collectList()
                    .block();

            if (changes == null || changes.isEmpty()) {
                log.debug("[cache-sync] no changes since {}", sinceParam);
            } else {
                int put = 0, evict = 0;
                LocalDateTime now = LocalDateTime.now();

                for (ShortUrlResponse item : changes) {
                    if (isDeletedOrExpired(item, now)) {
                        cacheService.evict(item.getShortKey());
                        evict++;
                    } else {
                        cacheService.put(item);
                        put++;
                    }
                }
                log.info("[cache-sync] {} changes applied (put={}, evict={})", changes.size(), put, evict);
            }

            lastSyncTime.set(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("[cache-sync] incremental sync failed, will retry in 5m. cause: {}", e.getMessage());
        }
    }

    private void fullLoad() {
        log.info("[cache-sync] Starting full cache load...");
        try {
            List<ShortUrlResponse> all = webClient.get()
                    .uri("/api/internal/short-urls/all")
                    .retrieve()
                    .bodyToFlux(ShortUrlResponse.class)
                    .collectList()
                    .block();

            if (all != null) {
                all.forEach(cacheService::put);
                lastSyncTime.set(LocalDateTime.now());
                log.info("[cache-sync] Full load completed. {} items cached.", all.size());
            }
        } catch (Exception e) {
            log.warn("[cache-sync] Full load failed — will retry on next scheduled sync. cause: {}", e.getMessage());
        }
    }

    private boolean isDeletedOrExpired(ShortUrlResponse item, LocalDateTime now) {
        // admin 에서 soft-delete 된 항목
        if (Boolean.TRUE.equals(item.getDeleted())) {
            return true;
        }
        // 만료 시각이 지난 항목
        if (item.getExpiredAt() == null) return false;
        try {
            LocalDateTime expiry = LocalDateTime.parse(item.getExpiredAt());
            return expiry.isBefore(now);
        } catch (Exception e) {
            return false;
        }
    }
}
