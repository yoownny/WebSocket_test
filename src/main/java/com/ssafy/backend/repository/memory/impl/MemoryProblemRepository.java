package com.ssafy.backend.repository.memory.impl;

import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.repository.memory.api.ProblemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class MemoryProblemRepository implements ProblemRepository {

    private final Map<String, Problem> problems = new ConcurrentHashMap<>();

    // 문제 저장
    @Override
    public Problem save(Problem problem) {
        if (problem == null || problem.getProblemId() == null) {
            throw new IllegalArgumentException("Problem이나 problemId는 null일 수 없습니다");
        }

        problems.put(problem.getProblemId(), problem);
        log.debug("메모리 문제 저장 완료: problemId={}, title={}",
                problem.getProblemId(), problem.getTitle());

        return problem;
    }

    // 문제 ID로 문제 조화
    @Override
    public Optional<Problem> findById(String problemId) {
        if (problemId == null || problemId.trim().isEmpty()) {
            return Optional.empty();
        }

        Problem problem = problems.get(problemId);
        log.debug("메모리 문제 조회: problemId={}, found={}", problemId, problem != null);

        // null 여부와 관계없이 Optional로 감싸서 반환
        return Optional.ofNullable(problem);
    }

    // 저장된 모든 문제 리스트로 반환
    @Override
    public List<Problem> findAll() {
        List<Problem> allProblems = new ArrayList<>(problems.values());
        log.debug("전체 메모리 문제 조회: count={}", allProblems.size());

        return allProblems;
    }

    // 문제 삭제
    @Override
    public void delete(String problemId) {
        if (problemId == null || problemId.trim().isEmpty()) {
            log.warn("삭제할 problemId가 유효하지 않습니다: {}", problemId);
            return;
        }

        Problem removed = problems.remove(problemId);
        if (removed != null) {
            log.info("메모리 문제 삭제 완료: problemId={}, title={}",
                    problemId, removed.getTitle());
        } else {
            log.warn("삭제할 메모리 문제를 찾을 수 없습니다: problemId={}", problemId);
        }
    }
}