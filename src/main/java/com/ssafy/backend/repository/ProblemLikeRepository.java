package com.ssafy.backend.repository;

import com.ssafy.backend.entity.ProblemLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemLikeRepository extends JpaRepository<ProblemLike, Long> {

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
    void deleteByUserIdAndProblemId(Long userId, Long problemId);

    @Query("SELECT COUNT(pl) FROM ProblemLike pl WHERE pl.problemId = :problemId")
    Integer countLikesByProblemId(@Param("problemId") Long problemId);
}
