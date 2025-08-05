package com.ssafy.backend.problem.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEvaluateRequestDto {

    // DB 문제 ID (기존 문제 평가용)
    private Long problemId;

    // 메모리 문제 ID (창작 문제 평가용)
    private String memoryProblemId;

    @NotNull(message = "평가 여부는 필수입니다")
    private Boolean isLike; // 좋아요/싫어요

    // 메모리 문제 평가시에만 필요 (총 참가자 수)
    private Integer totalPlayers;

    /**
     * 메모리 문제 평가인지 확인
     */
    public boolean isMemoryProblemEvaluation() {
        return memoryProblemId != null && !memoryProblemId.trim().isEmpty();
    }

    /**
     * DB 문제 평가인지 확인
     */
    public boolean isDbProblemEvaluation() {
        return problemId != null;
    }

    /**
     * 유효한 평가 요청인지 검증
     * - 메모리 문제: totalPlayers가 필수이며 1명 이상이어야 함
     * - DB 문제: problemId만 있으면 충분
     */
    public boolean isValidRequest() {
        return (isMemoryProblemEvaluation() && totalPlayers != null && totalPlayers > 0)
                || isDbProblemEvaluation();
    }
}
