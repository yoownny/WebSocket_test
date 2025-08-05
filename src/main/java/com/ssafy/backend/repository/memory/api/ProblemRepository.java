package com.ssafy.backend.repository.memory.api;

import com.ssafy.backend.memory.Problem;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository {

    /**
     * 메모리 문제 저장
     */
    Problem save(Problem problem);

    /**
     * ID로 메모리 문제 조회
     */
    Optional<Problem> findById(String problemId);

    /**
     * 모든 메모리 문제 조회
     */
    List<Problem> findAll();

    /**
     * 메모리 문제 삭제
     */
    void delete(String problemId);

}