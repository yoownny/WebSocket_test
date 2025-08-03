package com.ssafy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_created_problems")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCreatedProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "created_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserCreatedProblem(Long userId, Long problemId, LocalDateTime createdAt) {
        this.userId = userId;
        this.problemId = problemId;
        this.createdAt = createdAt;
    }
}
