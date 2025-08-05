package com.ssafy.backend.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionResponseDto {
    private Long hostId;
    private QuestionRequestDto questionRequestDto;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class QuestionRequestDto {
        private String question;
        private Long senderId;
    }
}

