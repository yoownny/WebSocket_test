package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.ReadyState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Player {
    private final Long userId;
    private final String nickname;

    private PlayerRole role; // HOST, PARTICIPANT
    private PlayerState state; // READY, PLAYING, DISCONNECTED
    private ReadyState readyState; // READY, WAITING
    private int answerAttempts = 3;  // 남은 정답 시도 횟수

    public synchronized void decrementAnswerAttempt() {
        if (answerAttempts <= 0) {
            throw new IllegalStateException("정답 시도 횟수를 초과했습니다.");
        }
        answerAttempts--;
    }

    // 준비 상태 토글 메서드
    public void toggleReadyState() {
        this.readyState = (this.readyState == ReadyState.READY) ? ReadyState.WAITING : ReadyState.READY;
    }

    // 준비 상태 확인 메서드
    public boolean isReady() {
        return this.readyState == ReadyState.READY;
    }
}