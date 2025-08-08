package com.ssafy.backend.ranking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItem {

    private Long problemId;
    private String title;
    private Integer likes;
    private Integer playCount;
    private Double score;
    private Integer rank;

    // QueryDSL용 생성자
    public RankingItem(Long problemId, String title, Integer likes, Integer playCount) {
        this.problemId = problemId;
        this.title = title;
        this.likes = likes != null ? likes : 0;
        this.playCount = playCount != null ? playCount : 0;
        this.score = 0.0;  // 초기값 (나중에 계산)
        this.rank = 0;     // 초기값 (나중에 계산)
    }
}