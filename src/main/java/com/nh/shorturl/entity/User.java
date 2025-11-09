package com.nh.shorturl.entity;

import com.nh.shorturl.constants.SchemaConstants;
import com.nh.shorturl.entity.common.BaseEntity;
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
@Table(
        name = SchemaConstants.TABLE_PREFIX + "USER",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_USER_USERNAME_APIKEY", columnNames = {"USERNAME", "API_KEY"})
        }
)
@SQLDelete(sql = "UPDATE " + SchemaConstants.TABLE_PREFIX + "USER SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("자동 생성 ID")
    private Long id;

    @Column(nullable = false)
    @Comment("사용자")
    private String username;

    @Column(name = "GROUP_NAME")
    @Comment("사용자 그룹(고객사 명)")
    private String groupName;

    @Column(name = "API_KEY")
    @Comment("고객별 발급 API Key")
    private String apiKey;

    @Column(name = "REFRESH_TOKEN")
    @Comment("API Key 재발급용 Refresh Token")
    private String refreshToken;

    @Column(name = "IS_DEL", length = 1)
    @Comment("삭제 여부")
    @Builder.Default
    private Boolean deleted = false;

    @Column
    @Comment("삭제일")
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
        // 이 예제에서는 별도의 권한을 사용하지 않으므로 비어있는 리스트를 반환합니다.
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        // UserDetails의 getPassword는 사용자 비밀번호를 반환해야 합니다.
        // 여기서는 apiKey를 비밀번호처럼 사용합니다.
        return this.apiKey;
    }

    // getUsername()은 UserDetails의 메서드와 일치하므로 별도 구현이 필요 없습니다.

    @Override
    public boolean isAccountNonExpired() {
        // 계정이 만료되지 않았는지 리턴 (true: 만료안됨)
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정이 잠기지 않았는지 리턴 (true: 잠기지 않음)
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호가 만료되지 않았는지 리턴 (true: 만료안됨)
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정이 활성화(사용가능)인지 리턴 (true: 활성화)
        return deleted;
    }
}
