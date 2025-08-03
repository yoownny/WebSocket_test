package com.ssafy.backend.repository.memory.api;


import com.ssafy.backend.memory.type.Difficulty;
import com.ssafy.backend.memory.Problem;

import java.util.List;
import java.util.Optional;

// todo; 안쓰면 삭제 예정
public interface ProblemRepository {
    Problem save(Problem problem);
    Optional<Problem> findById(String problemId);
    List<Problem> findByGenre(String genre);
    List<Problem> findByDifficulty(Difficulty difficulty);
    List<Problem> findAll();
    Optional<Problem> findRandomProblem();
    void clearStore();
}