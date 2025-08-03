package com.ssafy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_genres")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProblemGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "genre_id", nullable = false)
    private Long genreId;
}
