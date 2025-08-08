package com.ssafy.backend.game.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndResponseDto {
    private String endReason; // "CORRECT_ANSWER" or "TIMEOUT" or "EXHAUSTED_ATTEMPTS"
    private WinnerInfoDto winnerInfo;
    private ProblemDto problem;
    private int totalQuestionCount;
    @Setter
    private String playTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinnerInfoDto {
        private Long winnerId;
        private String nickname;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemDto {
        private String title;
        private String content;
        private String guess;
        private String answer;
    }
}
