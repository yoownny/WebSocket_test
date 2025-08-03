package com.ssafy.backend.problem.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProblemListResponseDto {

    private List<ProblemSummaryDto> problemList;
    private Long nextCursor;
    private boolean hasNext;
}
