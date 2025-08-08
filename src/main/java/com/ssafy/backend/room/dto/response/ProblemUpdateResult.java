package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemUpdateResult {
    private Room room;
    private Problem updatedProblem;
    private Long updatedBy; // 변경한 사용자 ID
    private String updatedByNickname; // 변경한 사용자 닉네임

    public static ProblemUpdateResult success(Room room, Problem problem, Long userId, String nickname) {
        return ProblemUpdateResult.builder()
                .room(room)
                .updatedProblem(problem)
                .updatedBy(userId)
                .updatedByNickname(nickname)
                .build();
    }
}
