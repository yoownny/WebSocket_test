package com.ssafy.backend.repository;

import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import org.springframework.data.domain.Slice;


public interface ProblemRepositoryCustom {
    Slice<ProblemSummaryDto> searchProblems(ProblemSearchRequestDto requestDto);
}

