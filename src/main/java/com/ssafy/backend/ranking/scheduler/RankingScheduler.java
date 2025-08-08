package com.ssafy.backend.ranking.scheduler;

import com.ssafy.backend.ranking.dto.RankingItem;
import com.ssafy.backend.ranking.service.RankingCacheService;
import com.ssafy.backend.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingScheduler {

    private final RankingService rankingService;
    private final RankingCacheService rankingCacheService;

    /**
     * 10분마다 랭킹 캐시 갱신
     */
    @Scheduled(fixedRate = 600000)
    public void updateRankingCache() {
        try {
            log.info("🔄 랭킹 캐시 갱신 시작...");

            // 1. DB에서 랭킹 계산 (좋아요 COUNT 포함)
            List<RankingItem> ranking = rankingService.calculateRankingFromDB();

            // 2. Redis에 캐시 저장
            rankingCacheService.cacheRanking(ranking);

            log.info("✅ 랭킹 캐시 갱신 완료: {}개 문제", ranking.size());

        } catch (Exception e) {
            log.error("❌ 랭킹 캐시 갱신 실패", e);
        }
    }

    /**
     * 서버 시작 1분 후 초기 랭킹 로드
     */
    @Scheduled(initialDelay = 60000, fixedRate = Long.MAX_VALUE)
    public void initializeRankingCache() {
        log.info("🚀 서버 시작 - 초기 랭킹 캐시 로드");
        updateRankingCache();
    }
}