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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaConstants.TABLE_PREFIX
        + "CLIENT_ACCESS_KEY", uniqueConstraints = @UniqueConstraint(name = "UK_CLIENT_ACCESS_KEY_VALUE", columnNames = "KEY_VALUE"))
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX
        + "CLIENT_ACCESS_KEY SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class ClientAccessKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고유 번호")
    private Long id;

    @Column(nullable = false)
    @Comment("클라이언트명")
    private String name;

    @Column(name = "KEY_VALUE", nullable = false, unique = true)
    @Comment("클라이언트 키 값")
    private String keyValue;

    @Column(name = "ISSUED_BY")
    @Comment("발급자")
    private String issuedBy;

    @Column(length = 500)
    @Comment("설명 (비고)")
    private String description;

    @Column(name = "EXPIRES_AT")
    @Comment("만료 일시")
    private LocalDateTime expiresAt;

    @Column(name = "LAST_USED_AT")
    @Comment("최근 사용 일시")
    private LocalDateTime lastUsedAt;

    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    @Column(name = "IS_ACTIVE", length = 1)
    @Comment("활성 상태 (Y/N)")
    @Builder.Default
    private Boolean active = true;

    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    @Column(name = "IS_DEL", length = 1)
    @Comment("삭제 여부 (Y/N)")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    @Comment("삭제 일시")
    private LocalDateTime deletedAt;
}
