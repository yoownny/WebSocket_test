package com.ssafy.backend.game.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>(); // 종료 타이머
    private final Map<Long, Instant> gameStartTimes = new ConcurrentHashMap<>(); // 시작 시간

    // 게임 시작 시 타이머 등록
    public void startGameTimer(Long roomId, int timeLimit, Runnable onTimeout) {
        Instant startTime = Instant.now();
        Instant endTime = Instant.now().plusSeconds(timeLimit * 60L); // 시간 설정

        // 시작 시간 저장
        gameStartTimes.put(roomId, startTime);

        // 타이머 등록
        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            try {
                onTimeout.run(); // 종료 시 실행할 로직
            } finally {
                // 타이머 삭제
                gameTimers.remove(roomId);
                gameStartTimes.remove(roomId);
            }
        }, endTime);

        gameTimers.put(roomId, future);
    }

    // (수동 종료) 타이머 취소
    public void cancelGameTimer(Long roomId) {
        ScheduledFuture<?> future = gameTimers.remove(roomId);
        gameStartTimes.remove(roomId);
        if (future != null) {
            future.cancel(true);
        }
    }

    // 경과 시간 계산 (분:초 형식)
    public String getElapsedTimeFormatted(Long roomId) {
        Instant start = gameStartTimes.get(roomId);
        if (start == null) {
            return "00:00"; // 아직 시작 안 했거나 이미 끝난 경우
        }
        long elapsedSeconds = Instant.now().getEpochSecond() - start.getEpochSecond();
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}

