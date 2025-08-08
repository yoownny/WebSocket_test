package com.ssafy.backend.repository;

import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.ranking.dto.RankingItem;
import org.springframework.data.domain.Slice;
import java.util.List;


public interface ProblemRepositoryCustom {
    Slice<ProblemSummaryDto> searchProblems(ProblemSearchRequestDto requestDto);

    List<RankingItem> findAllProblemsForRanking();
}

