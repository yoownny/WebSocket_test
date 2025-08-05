package com.ssafy.backend.problem.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailResponseDto {

    private String problemId;
    private String title;
    private String content;
    private String answer;
    private List<String> genres;
    private String difficulty;
    private CreatorInfo creator;
    private Integer likes;
    private Integer playCount;
    private Integer successCount;
    private Double successRate;
    private String source;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private String userId;
        private String nickname;
    }
}