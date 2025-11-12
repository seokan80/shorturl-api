package com.nh.shorturl.repository;

import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 모든 단축 URL을 페이징하여 조회한다.
     * 삭제되지 않은 항목만 조회된다. (@SQLRestriction에 의해 자동 필터링)
     */
    Page<ShortUrl> findAll(Pageable pageable);

    /**
     * 특정 사용자가 생성한 단축 URL을 페이징하여 조회한다.
     * 삭제되지 않은 항목만 조회된다. (@SQLRestriction에 의해 자동 필터링)
     */
    Page<ShortUrl> findByUser(User user, Pageable pageable);
}