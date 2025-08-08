package com.ssafy.backend.room.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class RoomCreateRequest {
    private int maxPlayers;
    private int timeLimit;
    private ProblemInfo problemInfo;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProblemInfo {
        private String problemId;
        private String problemType; // "CUSTOM" 또는 "ORIGINAL"

        // setter 메서드 추가
        public void setProblemId(String problemId) {
            this.problemId = problemId;
        }

        public void setProblemType(String problemType) {
            this.problemType = problemType;
        }
    }
}
