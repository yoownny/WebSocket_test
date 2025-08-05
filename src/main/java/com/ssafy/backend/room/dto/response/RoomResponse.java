package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.type.RoomState;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class RoomResponse {
    private Long roomId;
    private int maxPlayers;
    private int currentPlayers;
    private int timeLimit;
    private RoomState state;
    private Long hostId;
    private List<PlayerResponse> players;
    private ProblemResponse problem;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProblemResponse {
        private String problemId;
        private String title;
        private String content;
        private String answer; // 방장에게만 제공, 로비에서는 null
        private List<String> genres;
        private String difficulty;
        private CreatorInfo creator;
        private String source;

        @Getter
        @Builder
        @AllArgsConstructor
        public static class CreatorInfo {
            private String id;
            private String nickname;
        }
    }

    // 기본은 정답 숨김
    public static RoomResponse from(Room room) {
        return from(room, false);
    }

    public static RoomResponse from(Room room, boolean includeAnswer) {
        ProblemResponse problemResponse = null;

        if (room.getSelectedProblem() != null) {
            Problem problem = room.getSelectedProblem();
            problemResponse = ProblemResponse.builder()
                    .problemId(problem.getProblemId())
                    .title(problem.getTitle())
                    .content(problem.getContent())
                    .answer(includeAnswer ? problem.getAnswer() : null)
                    .genres(problem.getGenre())
                    .difficulty(problem.getDifficulty().name())
                    .creator(ProblemResponse.CreatorInfo.builder()
                            .id(problem.getCreatorId().toString())
                            .nickname(problem.getNickname())
                            .build())
                    .source(problem.getSource().name())
                    .build();
        }

        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .maxPlayers(room.getMaxPlayers())
                .currentPlayers(room.getCurrentPlayerCount())
                .timeLimit(room.getTimeLimit())
                .state(room.getState())
                .hostId(room.getHostId())
                .players(room.getPlayers().values().stream()
                        .map(PlayerResponse::from)
                        .collect(Collectors.toList()))
                .problem(problemResponse)
                .build();
    }
}
