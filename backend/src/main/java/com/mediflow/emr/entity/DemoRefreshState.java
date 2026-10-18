package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demo_refresh_state")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemoRefreshState {
    @Id
    @Column(length = 160)
    private String id;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
