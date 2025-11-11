package com.nh.shorturl.admin.entity;

import com.nh.shorturl.admin.constants.SchemaConstants;
import com.nh.shorturl.admin.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX + "SHORT_URL SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class ShortUrl extends BaseEntity {

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

    @Comment("생성자")
    private String createBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    @Comment("생성자 User ID")
    private User user;

    @Column
    @Comment("만료일")
    private LocalDateTime expiredAt;

    @Column(name = "IS_DEL", length = 1)
    @Comment("삭제 여부")
    @Builder.Default
    private Boolean deleted = false;

    @Column
    @Comment("삭제일")
    private LocalDateTime deletedAt;

    public ShortUrl(String longUrl, String shortUrl, String createBy, LocalDateTime expiredAt) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createBy = createBy;
        this.expiredAt = expiredAt;
    }
}
