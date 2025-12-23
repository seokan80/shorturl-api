package com.nh.shorturl.admin.entity;

import com.nh.shorturl.admin.constants.SchemaConstants;
import com.nh.shorturl.admin.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaConstants.TABLE_PREFIX + "USER", uniqueConstraints = {
        @UniqueConstraint(name = "UK_USER_USERNAME_APIKEY", columnNames = { "USERNAME", "API_KEY" })
})
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX
        + "USER SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고유 번호")
    private Long id;

    @Column(nullable = false)
    @Comment("사용자명")
    private String username;

    @Column(name = "GROUP_NAME")
    @Comment("고객사명")
    private String groupName;

    @Column(name = "API_KEY")
    @Comment("API 인증 키")
    private String apiKey;

    @Column(name = "REFRESH_TOKEN")
    @Comment("인증 갱신용 토큰")
    private String refreshToken;

    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    @Column(name = "IS_DEL", length = 1)
    @Comment("삭제 여부 (Y/N)")
    @Builder.Default
    private Boolean deleted = false;

    @Column
    @Comment("삭제 일시")
    private LocalDateTime deletedAt;

    public User(String username, String groupName) {
        this.username = username;
        this.groupName = groupName;
    }

    public User(String username) {
        this(username, (String) null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.apiKey;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !deleted;
    }
}
