package com.ssafy.backend.game.dto;


import com.ssafy.backend.memory.type.AnswerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequestDto {
    private Long questionerId;
    private String question;
    private AnswerStatus answerStatus;
}