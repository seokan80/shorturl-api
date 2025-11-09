package com.nh.shorturl.entity;

import com.nh.shorturl.constants.SchemaConstants;
import com.nh.shorturl.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaConstants.TABLE_PREFIX + "CLIENT_ACCESS_KEY",
    uniqueConstraints = @UniqueConstraint(name = "UK_CLIENT_ACCESS_KEY_VALUE", columnNames = "KEY_VALUE"))
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX + "CLIENT_ACCESS_KEY SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class ClientAccessKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "KEY_VALUE", nullable = false, unique = true)
    private String keyValue;

    @Column(name = "ISSUED_BY")
    private String issuedBy;

    @Column(length = 500)
    private String description;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "LAST_USED_AT")
    private LocalDateTime lastUsedAt;

    @Column(name = "IS_ACTIVE")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "IS_DEL", length = 1)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;
}
