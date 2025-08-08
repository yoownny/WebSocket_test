package com.ssafy.backend.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

    private List<RankingItem> ranking;
    private Integer totalCount;
    private LocalDateTime lastUpdated;
}