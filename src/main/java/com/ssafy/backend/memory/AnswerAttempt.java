package com.ssafy.backend.memory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AnswerAttempt {
    private final Long userId;  // 정답 시도자
    private final String guess;  // 정답 내용
}