package com.ssafy.backend.problem.service;

import com.ssafy.backend.entity.ProblemLike;
import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.problem.dto.Request.ProblemCreateDto;
import com.ssafy.backend.problem.dto.Request.ProblemEvaluateRequestDto;
import com.ssafy.backend.repository.ProblemLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemEvaluateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MemoryProblemService memoryProblemService;
    private final ProblemService problemService;
    private final ProblemLikeRepository problemLikeRepository;

    // Redis Key Prefix 설정
    private static final String MEMORY_LIKED_USERS_PREFIX = "memory:liked_users:"; // 메모리 문제 좋아요 유저 Set
    private static final String MEMORY_EVALUATED_PREFIX = "memory:evaluated:"; // 메모리 문제 평가 완료 유저 Set
    private static final String DB_LIKED_USERS_PREFIX = "db:liked_users:"; // DB 문제 좋아요 유저 Set

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

    /**
     * 메모리 문제 평가 처리
     * - Redis에 좋아요한 유저 목록 관리
     * - 과반수 달성 시 DB 저장 및 ProblemLike 엔티티 생성
     */
    private boolean evaluateMemoryProblem(ProblemEvaluateRequestDto request, Long userId) {
        String memoryProblemId = request.getMemoryProblemId();

        // Redis Key 설정
        String evaluatedKey = MEMORY_EVALUATED_PREFIX + memoryProblemId;
        String likedUsersKey = MEMORY_LIKED_USERS_PREFIX + memoryProblemId;

        // 1. 중복 평가 방지
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(evaluatedKey, userId))) {
            log.warn("이미 평가한 사용자입니다. memoryProblemId={}, userId={}", memoryProblemId, userId);
        }

        // 2. 평가 완료 유저 Set에 추가
        redisTemplate.opsForSet().add(evaluatedKey, userId);

        // 3. 좋아요 처리
        if (request.getIsLike()) {
            // 좋아요한 유저 목록에 추가
            redisTemplate.opsForSet().add(likedUsersKey, userId);

            // 현재 좋아요 수 확인
            Long likeCount = redisTemplate.opsForSet().size(likedUsersKey);
            log.info("좋아요 수 체크: {} / {}", likeCount, request.getTotalPlayers());

            // 4. 과반수 이상 좋아요인지 확인 (과반수: (총인원 + 1) / 2)
            if (likeCount >= (request.getTotalPlayers() + 1) / 2) {
                return saveMemoryProblemToDatabase(memoryProblemId);
            }
        }

        return false;
    }

    /**
     * DB 문제 평가 처리
     * - Redis에 좋아요 집계
     * - 동시에 ProblemLike 엔티티에도 저장
     */
    private void evaluateDbProblem(ProblemEvaluateRequestDto request, Long userId) {
        Long problemId = request.getProblemId();
        String likedUsersKey = DB_LIKED_USERS_PREFIX + problemId;

        // 1. 중복 평가 방지
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likedUsersKey, userId))) {
            log.warn("이미 평가한 사용자입니다. problemId={}, userId={}", problemId, userId);
        }

        // 2. Redis에 좋아요 유저 추가
        redisTemplate.opsForSet().add(likedUsersKey, userId);

        // 3. ProblemLike 엔티티 저장
        saveProblemLike(userId, problemId);

        log.info("DB 문제 좋아요 추가: problemId={}, userId={}", problemId, userId);
    }

    /**
     * 문제 평가 여부 확인
     */
    public boolean isAlreadyEvaluated(ProblemEvaluateRequestDto request, Long userId) {
        // 메모리 문제 평가 여부 확인
        if (request.isMemoryProblemEvaluation()) {
            String evaluatedKey = MEMORY_EVALUATED_PREFIX + request.getMemoryProblemId();
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(evaluatedKey, userId));
        }
        // DB 문제 평가 여부 확인
        else if (request.isDbProblemEvaluation()) {
            String likedUsersKey = DB_LIKED_USERS_PREFIX + request.getProblemId();
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likedUsersKey, userId));
        }
        return false;
    }

    /**
     * 메모리 문제를 DB에 영구 저장
     * - 문제 저장
     * - 좋아요한 유저들의 ProblemLike 엔티티 생성
     * - 메모리 및 Redis 데이터 정리
     */
    private boolean saveMemoryProblemToDatabase(String memoryProblemId) {
        try {
            // 1. 메모리 문제 조회
            Problem memoryProblem = memoryProblemService.findById(memoryProblemId);

            // 2. DTO로 변환
            ProblemCreateDto createDto = ProblemCreateDto.fromMemoryProblem(memoryProblem);

            // 3. DB에 문제 저장 (entity.Problem, memory,Problem 이름 수정 필요)
            com.ssafy.backend.entity.Problem savedProblem = problemService.create(createDto);
            Long savedProblemId = savedProblem.getId();
            log.info("메모리 문제 DB 저장 완료: memoryId={}, dbId={}", memoryProblemId, savedProblemId);

            // 4. 좋아요한 유저들을 ProblemLike 엔티티에 저장
            saveMemoryProblemLikes(memoryProblemId, savedProblemId);

            // 5. Redis에 DB 문제로 이관 (이후 평가는 DB 문제 로직 사용)
            migrateToDbEvaluation(memoryProblemId, savedProblemId);

            // 6. 메모리 문제 관련 데이터 정리
            deleteMemory(memoryProblemId);

            return true;
        } catch (Exception e) {
            log.error("메모리 문제 DB 저장 실패: {}", memoryProblemId, e);
            return false;
        }
    }

    /**
     * 메모리 문제의 좋아요 정보를 ProblemLike 엔티티에 저장
     */
    private void saveMemoryProblemLikes(String memoryProblemId, Long savedProblemId) {
        String likedUsersKey = MEMORY_LIKED_USERS_PREFIX + memoryProblemId;

        // 좋아요한 유저들 조회
        Set<Object> likedUsers = redisTemplate.opsForSet().members(likedUsersKey);

        if (likedUsers != null && !likedUsers.isEmpty()) {

            for (Object userIdObj : likedUsers) {
                try {
                    Long userId = Long.parseLong(userIdObj.toString());
                    saveProblemLike(userId, savedProblemId);
                    log.debug("메모리 문제 좋아요 저장: userId={}, problemId={}", userId, savedProblemId);
                } catch (NumberFormatException e) {
                    log.warn("유효하지 않은 userId 형식: {}", userIdObj, e);
                }
            }
        }
    }

    /**
     * 메모리 문제의 Redis 좋아요 유저 데이터를 DB 문제 Redis 키로 복사
     * - 이후부터는 DB 문제 평가 로직을 사용
     */
    private void migrateToDbEvaluation(String memoryProblemId, Long savedProblemId) {
        String memoryLikedUsersKey = MEMORY_LIKED_USERS_PREFIX + memoryProblemId;
        String dbLikedUsersKey = DB_LIKED_USERS_PREFIX + savedProblemId;

        // 메모리 문제의 좋아요 유저들을 DB 문제 키로 복사
        Set<Object> likedUsers = redisTemplate.opsForSet().members(memoryLikedUsersKey);
        if (likedUsers != null && !likedUsers.isEmpty()) {
            for (Object userId : likedUsers) {
                redisTemplate.opsForSet().add(dbLikedUsersKey, userId);
            }
            log.info("좋아요 정보를 DB 문제 평가 방식으로 이관 완료: {}명", likedUsers.size());
        }
    }

    /**
     * 메모리 문제 관련 데이터 정리
     */
    private void deleteMemory(String memoryProblemId) {
        // 메모리에서 문제 삭제
        memoryProblemService.deleteById(memoryProblemId);

        // Redis 데이터 삭제
        redisTemplate.delete(MEMORY_LIKED_USERS_PREFIX + memoryProblemId);
        redisTemplate.delete(MEMORY_EVALUATED_PREFIX + memoryProblemId);

        log.info("메모리 문제 데이터 정리 완료: {}", memoryProblemId);
    }


    /**
     * ProblemLike 엔티티 저장
     */
    public void saveProblemLike(Long userId, Long problemId) {
        if (!problemLikeRepository.existsByUserIdAndProblemId(userId, problemId)) {
            ProblemLike problemLike = ProblemLike.builder()
                    .userId(userId)
                    .problemId(problemId)
                    .build();
            problemLikeRepository.save(problemLike);
            log.debug("ProblemLike 저장: userId={}, problemId={}", userId, problemId);
        } else {
            log.debug("ProblemLike 이미 존재: userId={}, problemId={}", userId, problemId);
        }
    }

}