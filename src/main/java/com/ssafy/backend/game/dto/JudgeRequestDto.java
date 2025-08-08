package com.ssafy.backend.game.dto;

import com.ssafy.backend.memory.type.AnswerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRequestDto {
    private Long senderId;
    private String guess;
    private AnswerStatus answerStatus;
}
