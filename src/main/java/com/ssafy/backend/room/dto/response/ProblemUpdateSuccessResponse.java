package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Problem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemUpdateSuccessResponse {
    private RoomResponse.ProblemResponse problem;
    private String message;

    public static ProblemUpdateSuccessResponse from(Problem problem, boolean includeAnswer) {
        RoomResponse.ProblemResponse problemResponse = null;

        if (problem != null) {
            problemResponse = RoomResponse.ProblemResponse.builder()
                    .problemId(problem.getProblemId())
                    .title(problem.getTitle())
                    .content(problem.getContent())
                    .answer(includeAnswer ? problem.getAnswer() : null)
                    .genres(problem.getGenre())
                    .difficulty(problem.getDifficulty().name())
                    .creator(RoomResponse.ProblemResponse.CreatorInfo.builder()
                            .id(problem.getCreatorId().toString())
                            .nickname(problem.getNickname())
                            .build())
                    .source(problem.getSource().name())
                    .build();
        }

        return ProblemUpdateSuccessResponse.builder().problem(problemResponse).message("문제가 성공적으로 변경되었습니다.").build();
    }
}
