package com.nh.shorturl.admin.entity;

import com.nh.shorturl.admin.constants.SchemaConstants;
import com.nh.shorturl.admin.entity.common.BaseEntity;
import com.nh.shorturl.type.BotType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = SchemaConstants.TABLE_PREFIX + "SHORT_URL", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "SHORT_URL" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX
        + "SHORT_URL SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class ShortUrl extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고유 번호")
    private Long id;

    @Column(nullable = false, length = 2000)
    @Comment("원본 URL")
    private String longUrl;

    @Column(nullable = false, length = 100)
    @Comment("단축 키")
    private String shortUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    @Comment("생성 관리자")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ACCESS_KEY_ID")
    @Comment("클라이언트 키 (비회원 발급용)")
    private ClientAccessKey clientAccessKey;

    @Column
    @Comment("만료 일시")
    private LocalDateTime expiredAt;

    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    @Column(name = "IS_DEL", length = 1)
    @Comment("삭제 여부 (Y/N)")
    @Builder.Default
    private Boolean deleted = false;

    @Column
    @Comment("삭제 일시")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("봇 구분 (CALLBOT, CHATBOT)")
    private BotType botType;

    @Column(length = 100)
    @Comment("봇 서비스 식별 키 (전화번호/세션키)")
    private String botServiceKey;

    @Column(length = 50)
    @Comment("설문 식별 ID")
    private String surveyId;

    @Column(length = 20)
    @Comment("설문 버전")
    private String surveyVer;

    public ShortUrl(String longUrl, String shortUrl, LocalDateTime expiredAt) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.expiredAt = expiredAt;
    }
}
