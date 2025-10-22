package com.nh.shorturl.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 이 클래스는 테이블로 매핑되지 않고, 자식 클래스에게 필드를 상속해주는 역할만 합니다.
@EntityListeners(AuditingEntityListener.class) // JPA Auditing 기능을 활성화합니다.
public abstract class BaseEntity {

    @CreatedDate // Entity가 생성될 때의 시간을 자동으로 저장합니다.
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // Entity가 수정될 때의 시간을 자동으로 저장합니다.
    private LocalDateTime updatedAt;
}
