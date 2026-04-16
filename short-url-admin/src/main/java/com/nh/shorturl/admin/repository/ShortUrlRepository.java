package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    /** 단축 URL 문자열로 엔티티를 조회. */
    Optional<ShortUrl> findByShortUrl(String shortUrl);

    /** 단축 URL 키 존재 여부. */
    boolean existsByShortUrl(String shortUrl);

    /** 전체 페이징 조회 (@SQLRestriction 으로 soft-delete 자동 필터). */
    @Override
    @NonNull
    Page<ShortUrl> findAll(@NonNull Pageable pageable);

    /**
     * since 이후에 생성·수정·삭제된 항목을 반환한다.
     * @SQLRestriction 을 우회하여 소프트 삭제된 항목도 포함해야 하므로 nativeQuery 사용.
     * redirect 서버의 증분 폴링용.
     */
    @Query(value = "SELECT * FROM TBL_SHORT_URL WHERE UPDATED_AT >= :since OR DELETED_AT >= :since",
           nativeQuery = true)
    List<ShortUrl> findChangedSince(@Param("since") LocalDateTime since);
}
