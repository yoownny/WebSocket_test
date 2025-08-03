package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.AnswerStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QnA {
    private final Long questionerId; // 질문자 ID
    private final String question; // 질문 내용
    private final AnswerStatus answer; // 답변 (예/아니오/상관없음/대기중)
}
