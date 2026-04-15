package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

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
}
