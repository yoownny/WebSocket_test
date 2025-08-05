package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.AnswerStatus;
import lombok.*;

@Getter
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class QnA {
    private HistoryType historyType; // 질문, 정답 시도
    private final Long questionerId; // 질문자 ID
    private final String question; // 질문 내용
    @Setter
    private AnswerStatus answer; // 답변 (예/아니오/상관없음/대기중)
}
