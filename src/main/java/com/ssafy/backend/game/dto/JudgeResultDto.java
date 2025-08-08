package com.ssafy.backend.game.dto;

import com.ssafy.backend.memory.QnA;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultDto {
    private Boolean isEnd;
    private Boolean hasRemainGuess;
    private EndResponseDto endResponseDto;
    private QnA qnA;
    private AnswerResultDto.GuessDto guessDto;
    private NextTurnDto nextTurnDto;
}
