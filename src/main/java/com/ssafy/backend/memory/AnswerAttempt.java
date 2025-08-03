package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.AnswerStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AnswerAttempt {
    private final long userId;  // 정답 시도자
    private final String answerText;  // 정답 내용
    private AnswerStatus status = AnswerStatus.PENDING; // PENDING, CORRECT, INCORRECT
}