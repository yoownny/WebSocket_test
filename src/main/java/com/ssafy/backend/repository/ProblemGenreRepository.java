package com.ssafy.backend.repository;

import com.ssafy.backend.entity.ProblemGenre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemGenreRepository extends JpaRepository<ProblemGenre,Long> {

    List<ProblemGenre> findAllByProblemId(Long problemId);

}
