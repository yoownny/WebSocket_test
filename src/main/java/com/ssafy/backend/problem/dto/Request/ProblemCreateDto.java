package com.ssafy.backend.problem.dto.Request;

import com.ssafy.backend.memory.Problem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemCreateDto {
    private String title;
    private String content;
    private String answer;
    private List<String> genres;
    private String difficulty;
    private CreatorDto creator;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorDto {
        private Long id;
        private String nickname;
    }

    /**
     * memory.Problem 객체를 기반으로 ProblemCreateDto로 변환하는 정적 메서드
     * @param memoryProblem 메모리에 저장된 문제 객체
     * @return ProblemCreateDto 객체
     */
    public static ProblemCreateDto fromMemoryProblem(Problem memoryProblem) {
        return ProblemCreateDto.builder()
                .title(memoryProblem.getTitle())
                .content(memoryProblem.getContent())
                .answer(memoryProblem.getAnswer())
                .genres(memoryProblem.getGenre())
                .difficulty(memoryProblem.getDifficulty().name())
                .creator(CreatorDto.builder()
                        .id(memoryProblem.getCreatorId())
                        .nickname(memoryProblem.getNickname())
                        .build())
                .build();
    }
}