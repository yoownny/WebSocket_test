package com.ssafy.backend.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

/**
 * 문제 평가 관리를 위한 Redis Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProblemEvaluationRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String EVALUATION_PREFIX = "evaluate:";

    /**
     * 사용자 평가 추가
     */
    public boolean addUserEvaluation(String problemId, Long userId) {
        String key = EVALUATION_PREFIX + problemId;
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        // 중복 평가 방지
        if (Boolean.TRUE.equals(ops.isMember(key, userId))) {
            return false;
        }

        Long result = ops.add(key, userId);
        // TTL 수정: 1일 (24시간)
        redisTemplate.expire(key, Duration.ofDays(1));

        return result != null && result > 0;
    }

    /**
     * 현재 좋아요 수 조회
     */
    public Long getCurrentLikeCount(String problemId) {
        String key = EVALUATION_PREFIX + problemId;
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Long count = ops.size(key);
        return count != null ? count : 0L;
    }

    /**
     * 사용자 평가 여부 확인
     */
    public boolean hasUserEvaluated(String problemId, Long userId) {
        String key = EVALUATION_PREFIX + problemId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId));
    }

    /**
     * 평가 데이터 삭제
     */
    public boolean deleteEvaluations(String problemId) {
        String key = EVALUATION_PREFIX + problemId;
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 모든 평가 키 조회
     */
    public Set<String> getAllEvaluationKeys() {
        return redisTemplate.keys(EVALUATION_PREFIX + "*");
    }

    /**
     * 평가 데이터 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ProblemEvaluationData {
        private String problemId;
        private String redisKey;
        private Integer likeCount;
        private Set<Object> evaluatedUsers;
        private Long ttlSeconds;
    }
}