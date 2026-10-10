package com.mediflow.emr.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    /**
     * 동일 기록을 여러 사용자가 동시에 수정할 때 마지막 저장이 앞선 변경을
     * 조용히 덮어쓰지 않도록 하는 낙관적 잠금 버전이다.
     */
    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
