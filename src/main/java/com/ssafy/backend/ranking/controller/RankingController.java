package com.ssafy.backend.ranking.controller;

import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.common.response.SuccessResponse;
import com.ssafy.backend.exception.SuccessCode;
import com.ssafy.backend.ranking.dto.RankingItem;
import com.ssafy.backend.ranking.dto.RankingResponse;
import com.ssafy.backend.ranking.service.RankingCacheService;
import com.ssafy.backend.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "랭킹 관리", description = "문제 랭킹 조회 API")
public class RankingController {

    private final RankingService rankingService;
    private final RankingCacheService rankingCacheService;

    /**
     * 전체 문제 랭킹 조회
     */
    @GetMapping
    @Operation(
            summary = "전체 문제 랭킹 조회",
            description = "좋아요와 플레이 수를 기반으로 한 전체 문제 랭킹을 조회합니다."
    )
    public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
            @Parameter(description = "조회할 랭킹 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 1. Redis 캐시에서 조회
            List<RankingItem> ranking = rankingCacheService.getCachedRanking(limit);

            if (ranking == null) {
                // 2. 캐시 미스시 DB에서 계산
                log.warn("⚠️ 랭킹 캐시 미스 - DB에서 조회");
                ranking = rankingService.calculateRankingFromDB();

                // DB 조회 후 캐시에 저장
                if (ranking != null && !ranking.isEmpty()) {
                    log.info("💾 랭킹 데이터를 캐시에 저장합니다...");
                    rankingCacheService.cacheRanking(ranking);
                }

                // 상위 limit개만 자르기
                if (ranking != null && ranking.size() > limit) {
                    ranking = ranking.subList(0, limit);
                }
            }

            /// 3. 응답 생성
            RankingResponse response = rankingService.buildRankingResponse(
                    ranking != null ? ranking : Collections.emptyList()
            );

            return ApiResponse.success(
                    SuccessCode.GET_SUCCESS.getStatus(),
                    SuccessCode.GET_SUCCESS.getMessage(),
                    response
            );

        } catch (Exception e) {
            log.error("랭킹 조회 실패", e);
            return ApiResponse.success(
                    SuccessCode.GET_SUCCESS.getStatus(),
                    "랭킹을 불러올 수 없습니다",
                    RankingResponse.builder()
                            .ranking(Collections.emptyList())
                            .totalCount(0)
                            .build()
            );
        }
    }

    /**
     * 랭킹 캐시 강제 갱신 (관리자용)
     */
    @PostMapping("/refresh")
    @Operation(summary = "랭킹 캐시 강제 갱신")
    public ResponseEntity<SuccessResponse<String>> refreshRanking() {
        try {
            List<RankingItem> ranking = rankingService.calculateRankingFromDB();
            rankingCacheService.cacheRanking(ranking);

            return ApiResponse.success(
                    SuccessCode.UPDATE_SUCCESS.getStatus(),
                    "랭킹 캐시가 갱신되었습니다",
                    "갱신 완료: " + ranking.size() + "개 문제"
            );

        } catch (Exception e) {
            log.error("랭킹 강제 갱신 실패", e);
            return ApiResponse.success(
                    SuccessCode.UPDATE_SUCCESS.getStatus(),
                    "랭킹 갱신에 실패했습니다",
                    "오류: " + e.getMessage()
            );
        }
    }
}