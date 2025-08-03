package com.ssafy.backend.repository;

import com.ssafy.backend.entity.ProblemGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemGenreRepository extends JpaRepository<ProblemGenre,Long> {
}
