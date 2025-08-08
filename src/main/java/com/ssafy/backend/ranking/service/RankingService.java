package com.ssafy.backend.ranking.service;

import com.ssafy.backend.ranking.dto.RankingItem;
import com.ssafy.backend.ranking.dto.RankingResponse;
import com.ssafy.backend.repository.ProblemRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final ProblemRepositoryCustom problemRepositoryCustom;

    /**
     * DB에서 랭킹 계산 (Redis 캐시 미스시 사용)
     */
    public List<RankingItem> calculateRankingFromDB() {
        try {
            log.info("🔄 DB에서 랭킹 계산 시작...");

            // 1. DB에서 RankingItem 조회 (좋아요 COUNT 포함)
            List<RankingItem> problems = problemRepositoryCustom.findAllProblemsForRanking();


            // 2. 점수 계산 & 정렬
            List<RankingItem> rankedProblems = problems.stream()
                    .map(this::calculateScore)
                    .sorted(
                            Comparator.comparing(RankingItem::getScore).reversed()
                                    .thenComparing(RankingItem::getLikes, Comparator.reverseOrder())
                                    .thenComparing(RankingItem::getPlayCount, Comparator.reverseOrder())
                    )
                    .limit(50) // 상위 50개만
                    .collect(Collectors.toList());

            // 3. 순위 부여
            AtomicInteger rank = new AtomicInteger(1);
            List<RankingItem> finalRanking = rankedProblems.stream()
                    .map(item -> RankingItem.builder()
                            .problemId(item.getProblemId())
                            .title(item.getTitle())
                            .likes(item.getLikes())
                            .playCount(item.getPlayCount())
                            .score(item.getScore())
                            .rank(rank.getAndIncrement())
                            .build())
                    .collect(Collectors.toList());

            log.info("✅ 랭킹 계산 완료: {}개 문제", finalRanking.size());
            return finalRanking;

        } catch (Exception e) {
            log.error("❌ 랭킹 계산 실패", e);
            throw new RuntimeException("랭킹 계산 중 오류 발생", e);
        }
    }

    /**
     * 로그 스케일 점수 계산: 좋아요 × 3.0 + log(플레이수 + 1) × 2.0
     */
    private RankingItem calculateScore(RankingItem item) {
        double likeScore = item.getLikes() * 3.0;
        double playScore = Math.log(item.getPlayCount() + 1) * 2.0;
        double finalScore = likeScore + playScore;

        return RankingItem.builder()
                .problemId(item.getProblemId())
                .title(item.getTitle())
                .likes(item.getLikes())
                .playCount(item.getPlayCount())
                .score(finalScore)
                .build();
    }

    /**
     * 랭킹 응답 생성
     */
    public RankingResponse buildRankingResponse(List<RankingItem> ranking) {
        return RankingResponse.builder()
                .ranking(ranking)
                .totalCount(ranking.size())
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}