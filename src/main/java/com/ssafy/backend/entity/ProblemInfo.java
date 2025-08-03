package com.ssafy.backend.entity;

import com.ssafy.backend.common.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProblemInfo {

    @Id
    @Column(name = "problem_id")
    private Long id;

    @Column(nullable = false)
    private Integer likes;

    @Column(name = "play_count", nullable = false)
    private Integer playCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "success_rate", nullable = false)
    private Double successRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
}
