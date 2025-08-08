package com.ssafy.backend.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.backend.common.enums.Difficulty;
import com.ssafy.backend.common.enums.Source;
import com.ssafy.backend.entity.*;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.ranking.dto.RankingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProblemRepositoryImpl implements ProblemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ProblemSummaryDto> searchProblems(ProblemSearchRequestDto requestDto) {

        // QueryDSL Q타입 객체 선언
        QProblem problem = QProblem.problem;
        QProblemInfo info = QProblemInfo.problemInfo;
        QProblemGenre problemGenre = QProblemGenre.problemGenre;
        QGenre genre = QGenre.genre;
        QUserCreatedProblem ucp = QUserCreatedProblem.userCreatedProblem;
        QUser user = QUser.user;

        // 동적 쿼리를 위한 BooleanBuilder 생성
        BooleanBuilder builder = new BooleanBuilder();

        int pageSize = requestDto.getSize() != null ? requestDto.getSize() : 10;

        // 커서 기반 페이징 처리
        if (requestDto.getCursor() != null) {
            builder.and(problem.id.lt(requestDto.getCursor()));
        }

        // 필터 조건 (난이도, 출처, 키워드, 장르)
        if (requestDto.getDifficulty() != null) {
            builder.and(info.difficulty.eq(Difficulty.valueOf(requestDto.getDifficulty())));
        }

        // 특정 문제 ID 조회
        if (requestDto.getProblemId() != null) {
            builder.and(problem.id.eq(requestDto.getProblemId()));
        }

        if (requestDto.getSource() != null) {
            builder.and(problem.source.eq(Source.valueOf(requestDto.getSource())));
        }

        if (requestDto.getKeyword() != null && !requestDto.getKeyword().isBlank()) {
            builder.and(problem.title.containsIgnoreCase(requestDto.getKeyword()));
        }

        if (requestDto.getGenre() != null && !requestDto.getGenre().isEmpty()) {
            List<Long> genreFilteredIds = queryFactory
                    .select(problemGenre.problemId)
                    .from(problemGenre)
                    .join(genre).on(problemGenre.genreId.eq(genre.id))
                    .where(genre.name.in(requestDto.getGenre()))
                    .fetch();

            if (genreFilteredIds.isEmpty()) {
                return new SliceImpl<>(List.of(), PageRequest.of(0, pageSize), false);
            }

            builder.and(problem.id.in(genreFilteredIds));
        }

        // 정렬 조건
        OrderSpecifier<?> order;
        switch (requestDto.getSort()) {
            case "popular" -> order = info.likes.desc(); // 인기순
            case "difficulty" -> order = info.difficulty.desc(); // 난이도순
            case "rating" -> order = info.successRate.desc(); // 성공률순
            default -> order = problem.id.desc();  // 최신순
        }

        // 문제 목록 조회 (Problem + ProblemInfo 조인)
        List<Problem> problems = queryFactory
                .selectFrom(problem)
                .join(info).on(problem.id.eq(info.id)).fetchJoin()
                .where(builder)
                .orderBy(order)
                .limit(pageSize + 1)
                .fetch();

        // hasNext 처리 (다음 페이지 존재 여부)
        boolean hasNext = problems.size() > pageSize;
        if (hasNext) problems.remove(pageSize);

        // 문제 ID 목록 추출
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();

        // 문제 ID로 Genre 매핑 조회
        Map<Long, List<String>> genreMap = queryFactory
                .select(problemGenre.problemId, genre.name)
                .from(problemGenre)
                .join(genre).on(problemGenre.genreId.eq(genre.id))
                .where(problemGenre.problemId.in(problemIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(problemGenre.problemId),
                        Collectors.mapping(tuple -> tuple.get(genre.name), Collectors.toList())
                ));

        // 문제 ID로 User(작성자) 매핑 조회
        Map<Long, ProblemSummaryDto.creatorInfo> creatorMap = queryFactory
                .select(problem.id, user.userId, user.nickname)
                .from(problem)
                .join(ucp).on(problem.id.eq(ucp.problemId))
                .join(user).on(ucp.userId.eq(user.userId))
                .where(problem.id.in(problemIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(problem.id),
                        tuple -> new ProblemSummaryDto.creatorInfo(
                                tuple.get(user.userId),
                                tuple.get(user.nickname)
                        ),
                        (existing, replacement) -> existing
                ));

        // ProblemInfo 엔티티 별도 조회 (likes, difficulty 등)
        Map<Long, ProblemInfo> infoMap = queryFactory
                .selectFrom(info)
                .where(info.id.in(problemIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(ProblemInfo::getId, i -> i));

        // DTO 변환
        List<ProblemSummaryDto> result = problems.stream()
                .map(p -> {
                    ProblemInfo pi = infoMap.get(p.getId());
                    return ProblemSummaryDto.builder()
                            .problemId(p.getId().toString())
                            .title(p.getTitle())
                            .content(p.getContent())
                            .answer(p.getAnswer())
                            .genres(genreMap.getOrDefault(p.getId(), List.of()))
                            .difficulty(pi != null ? pi.getDifficulty().name() : null)
                            .creator(creatorMap.get(p.getId()))
                            .likes(pi != null ? pi.getLikes() : 0)
                            .source(p.getSource().name().toLowerCase())
                            .build();
                })
                .toList();

        // Slice 형태로 반환
        return new SliceImpl<>(result, PageRequest.of(0, pageSize), hasNext);
    }

    @Override
    public List<RankingItem> findAllProblemsForRanking() {
        QProblem problem = QProblem.problem;
        QProblemInfo info = QProblemInfo.problemInfo;
        QProblemLike likes = QProblemLike.problemLike;

        return queryFactory
                .select(Projections.constructor(RankingItem.class,
                        problem.id,
                        problem.title,
                        // 좋아요 수 서브쿼리로 COUNT
                        JPAExpressions.select(likes.count().intValue())
                                .from(likes)
                                .where(likes.problemId.eq(problem.id)),
                        info.playCount
                ))
                .from(problem)
                .join(info).on(problem.id.eq(info.id))
                .where(
                        info.playCount.goe(1) // 최소 1회 이상 플레이
                )
                .fetch();
    }
}
