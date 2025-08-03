package com.ssafy.backend.repository.memory.impl;

import com.ssafy.backend.memory.type.Difficulty;
import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.repository.memory.api.ProblemRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// todo; 안쓰면 삭제 예정
@Repository
public class MemoryProblemRepository implements ProblemRepository {

    private final ConcurrentHashMap<String, Problem> problems = new ConcurrentHashMap<>();
    private final Random random = new Random();
    // 인덱스 추가
    private final Map<String, List<Problem>> problemsByGenre = new ConcurrentHashMap<>();
    private final Map<Difficulty, List<Problem>> problemsByDifficulty = new ConcurrentHashMap<>();


    public MemoryProblemRepository() {
        initializeDefaultProblems();
    }

    @Override
    public Problem save(Problem problem) {
        problems.put(problem.getProblemId(), problem);
        // 문제가 추가/수정될 때마다 인덱스를 업데이트해야 함
        rebuildIndexes(); // 간단한 구현. 더 복잡한 시스템에서는 해당 문제만 부분적으로 업데이트
        return problem;
    }

    @Override
    public Optional<Problem> findById(String problemId) {
        return Optional.ofNullable(problems.get(problemId));
    }

    @Override
    public List<Problem> findByGenre(String genre) {
        // 인덱스를 사용하여 O(1) 조회
        return problemsByGenre.getOrDefault(genre, Collections.emptyList());
    }

    @Override
    public List<Problem> findByDifficulty(Difficulty difficulty) {
        // 인덱스를 사용하여 O(1) 조회
        return problemsByDifficulty.getOrDefault(difficulty, Collections.emptyList());
    }

    @Override
    public List<Problem> findAll() {
        return new ArrayList<>(problems.values());
    }

    @Override
    public Optional<Problem> findRandomProblem() {
        if (problems.isEmpty()) {
            return Optional.empty();
        }
        // 전체 value를 복사하는 대신 keySet을 배열로 만들어 랜덤 접근
        Object[] keys = problems.keySet().toArray();
        String randomKey = (String) keys[random.nextInt(keys.length)];
        return Optional.ofNullable(problems.get(randomKey));
    }

    @Override
    public void clearStore() {
        problems.clear();
        initializeDefaultProblems();
    }

    private void initializeDefaultProblems() {
        // 기본 문제들 초기화
        problems.put("PROBLEM_001", new Problem(
                "PROBLEM_001",
                "미스터리 살인사건",
                "깊은 밤, 고급 펜션에서 벌어진 의문의 살인사건. 범인을 찾아보세요.",
                "집사",
                "추리",
                Difficulty.NORMAL,
                null
        ));

        problems.put("PROBLEM_002", new Problem(
                "PROBLEM_002",
                "사라진 보물",
                "해적의 보물이 숨겨진 무인도. 보물의 위치를 찾아보세요.",
                "동굴 안 바위 뒤",
                "모험",
                Difficulty.HARD,
                null
        ));

        problems.put("PROBLEM_003", new Problem(
                "PROBLEM_003",
                "마법사의 수수께끼",
                "마법사가 낸 수수께끼를 풀어야 마을을 구할 수 있습니다.",
                "사랑",
                "판타지",
                Difficulty.EASY,
                null
        ));
        // 인덱스 재생성
        rebuildIndexes();
    }

    // 인덱스를 생성하는 헬퍼 메서드
    private void rebuildIndexes() {
        problemsByGenre.clear();
        problemsByDifficulty.clear();

        // GroupingBy를 사용하여 효율적으로 인덱스 생성
        problemsByGenre.putAll(problems.values().stream()
                .collect(Collectors.groupingBy(Problem::getGenre)));

        problemsByDifficulty.putAll(problems.values().stream()
                .collect(Collectors.groupingBy(Problem::getDifficulty)));
    }
}