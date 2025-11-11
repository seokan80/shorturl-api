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
    @Comment("자동 생성 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("Short URL ID")
    private ShortUrl shortUrl;

    @Comment("referer")
    private String referer;

    @Comment("userAgent")
    private String userAgent;

    @Comment("IP")
    private String ip;

    @Comment("Redirect 일시")
    private LocalDateTime redirectAt;
}