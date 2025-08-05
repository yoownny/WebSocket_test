package com.ssafy.backend.problem.service;

import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.problem.dto.Request.ProblemCreateDto;
import com.ssafy.backend.problem.dto.Request.ProblemEvaluateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemEvaluateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MemoryProblemService memoryProblemService;
    private final ProblemService problemService;

    // Redis Key Prefix 설정
    private static final String MEMORY_LIKE_PREFIX = "memory:like:";
    private static final String MEMORY_EVALUATED_PREFIX = "memory:evaluated:";
    private static final String DB_EVALUATE_PREFIX = "evaluate:";

    /**
     * 문제 평가 요청 처리
     * - 메모리 문제인지 DB 문제인지 구분하여 평가 분기
     * @param request 평가 요청 DTO
     * @param userId 평가하는 사용자 ID
     * @return 메모리 문제 과반수 이상 좋아요로 DB 저장 시 true
     */
    public boolean evaluate(ProblemEvaluateRequestDto request, Long userId) {
        if (request.isMemoryProblemEvaluation()) {
            return evaluateMemoryProblem(request, userId);
        } else if (request.isDbProblemEvaluation()) {
            evaluateDbProblem(request, userId);
            return false;
        }
        throw new IllegalArgumentException("유효하지 않은 평가 요청입니다.");
    }

    // 메모리 문제 평가 처리
    private boolean evaluateMemoryProblem(ProblemEvaluateRequestDto request, Long userId) {
        String memoryProblemId = request.getMemoryProblemId();

        // 1. Redis에 평가한 유저인지 확인 (중복 평가 방지)
        String evaluatedKey = MEMORY_EVALUATED_PREFIX + memoryProblemId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(evaluatedKey, userId))) {
            log.warn("이미 평가한 사용자입니다. memoryProblemId={}, userId={}", memoryProblemId, userId);
            return false;
        }

        // 2. 평가 유저 등록 (평가 완료 처리)
        redisTemplate.opsForSet().add(evaluatedKey, userId);

        // 3. 좋아요 처리
        if (request.getIsLike()) {
            String likeKey = MEMORY_LIKE_PREFIX + memoryProblemId;
            redisTemplate.opsForValue().increment(likeKey); // 좋아요 수 증가

            // 4. 과반수 이상 좋아요인지 확인
            long likeCount = getLikeCount(memoryProblemId);
            log.info("좋아요 수 체크: {} / {}", likeCount, request.getTotalPlayers());

            if (likeCount >= request.getTotalPlayers() / 2) {
                log.info("과반수 넘음. DB 저장 시도");
                return saveToDatabase(memoryProblemId);
            }
            log.info("✅ 메모리 문제 DB 저장 완료: {}", memoryProblemId);
        }

        return false;
    }

    // DB 문제 평가 처리
    private void evaluateDbProblem(ProblemEvaluateRequestDto request, Long userId) {
        String key = DB_EVALUATE_PREFIX + request.getProblemId();

        if (request.getIsLike()) {
            redisTemplate.opsForSet().add(key, userId);
        } else {
            redisTemplate.opsForSet().remove(key, userId);
        }
    }

    // 메모리 문제를 DB에 영구 저장
    private boolean saveToDatabase(String memoryProblemId) {
        try {
            // 1. 메모리 문제 조회
            Problem memoryProblem = memoryProblemService.findById(memoryProblemId);
            log.info("memoryProblem: {}", memoryProblem);

            // 2. DTO로 변환
            ProblemCreateDto createDto = ProblemCreateDto.fromMemoryProblem(memoryProblem);
            log.info("ProblemCreateDto: {}", createDto);

            // 3. DB 저장
            problemService.create(createDto);
            log.info("✅ 메모리 문제 DB 저장 완료: {}", memoryProblemId);

            // 4. 메모리 및 Redis 정리
            memoryProblemService.deleteById(memoryProblemId);
            redisTemplate.delete(MEMORY_LIKE_PREFIX + memoryProblemId);
            redisTemplate.delete(MEMORY_EVALUATED_PREFIX + memoryProblemId);

            return true;
        } catch (Exception e) {
            log.error("메모리 문제 DB 저장 실패: {}", memoryProblemId, e);
            return false;
        }
    }

    // Redis에 저장된 메모리 문제의 좋아요 수 가져오기
    public long getLikeCount(String memoryProblemId) {
        String likeKey = MEMORY_LIKE_PREFIX + memoryProblemId;
        Object value = redisTemplate.opsForValue().get(likeKey);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}