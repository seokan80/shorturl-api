package com.nh.shorturl.repository;

import com.nh.shorturl.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    /**
     * 단축 URL 문자열로 엔티티를 조회한다.
     */
    Optional<ShortUrl> findByShortUrl(String shortUrl);

    /**
     * 특정 단축 URL 키가 이미 존재하는지 확인한다.
     */
    boolean existsByShortUrl(String shortUrl);
}