package com.nh.shorturl.admin.entity;

import com.nh.shorturl.admin.constants.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = SchemaConstants.TABLE_PREFIX + "REDIRECTION" + SchemaConstants.HISTORY_TABLE_SUFFIX)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고유 번호")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("단축 URL 정보")
    private ShortUrl shortUrl;

    @Comment("레퍼러 (이전 페이지 주소)")
    private String referer;

    @Comment("User Agent (브라우저 정보)")
    private String userAgent;

    @Comment("접속 IP 주소")
    private String ip;

    @Column(length = 20)
    @Comment("디바이스 구분 (Mobile, Desktop 등)")
    private String deviceType;

    @Column(length = 50)
    @Comment("운영체제(OS)")
    private String os;

    @Column(length = 50)
    @Comment("브라우저 명")
    private String browser;

    @Column(length = 10)
    @Comment("접속 국가 코드")
    private String country;

    @Column(length = 100)
    @Comment("접속 도시 명")
    private String city;

    @Comment("리다이렉션 실행 일시")
    private LocalDateTime redirectAt;
}
