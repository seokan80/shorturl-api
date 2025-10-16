package com.nh.shorturl.entity;

import com.nh.shorturl.constants.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = SchemaConstants.TABLE_PREFIX + "SHORT_URL", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"SHORT_URL"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("자동 생성 ID")
    private Long id;

    @Column(nullable = false, length = 2000)
    @Comment("원본 URL")
    private String longUrl;

    @Column(nullable = false, length = 100)
    @Comment("단축 URL")
    private String shortUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("생성자 User ID")
    private User createBy;

    @Column
    @Comment("만료일")
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    @Comment("생성일")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ShortUrl(String longUrl, String shortUrl, User createBy, LocalDateTime expiredAt) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createBy = createBy;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
    }
}
