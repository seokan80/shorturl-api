package com.nh.shorturl.admin.entity;

import com.nh.shorturl.admin.constants.SchemaConstants;
import com.nh.shorturl.admin.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaConstants.TABLE_PREFIX + "REDIRECTION_CONFIG")
public class RedirectionConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FALLBACK_URL", length = 1000)
    private String fallbackUrl;

    @Column(name = "DEFAULT_HOST", length = 500)
    private String defaultHost;

    @Column(name = "SHOW_ERROR_PAGE")
    @Builder.Default
    private Boolean showErrorPage = true;

    @Column(name = "TRACKING_FIELDS", length = 500)
    private String trackingFields;
}
