package com.ssafy.backend.problem.service;

import com.ssafy.backend.common.enums.Difficulty;
import com.ssafy.backend.common.enums.Source;
import com.ssafy.backend.entity.*;
import com.ssafy.backend.problem.dto.ProblemCreateDto;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final ProblemGenreRepository problemGenreRepository;
    private final UserCreatedProblemRepository userCreatedProblemRepository;
    private final ProblemInfoRepository problemInfoRepository;

    private final ProblemRepositoryCustom problemRepositoryCustom;

    public Slice<ProblemSummaryDto> searchProblems(ProblemSearchRequestDto requestDto) {
        return problemRepositoryCustom.searchProblems(requestDto);
    }

    // 창작 문제 생성
    public Problem create(ProblemCreateDto dto) {

        // 1. 사용자 찾기
        // 요청한 유저의 socialId로 DB에서 유저 정보 조회
        User user = userRepository.findBySocialId(dto.getCreator().getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다."));

        // 문제 작성자의 역할이 USER이라면 문제 출처는 CUSTOM
        Source source;
        if (user.getRole().equals("USER")) {
            source = Source.CUSTOM;
        } else {
            source = Source.ORIGINAL;
        }

        // 2. 문제 저장
        // 문제 본문, 제목, 정답 등을 저장
        Problem problem = Problem.builder()
                .creatorId(user.getUserId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .answer(dto.getAnswer())
                .source(source)
                .createdAt(LocalDateTime.now())
                .build();

        Problem saved = problemRepository.save(problem);

        // 3. 문제 정보 저장
        // 난이도, 좋아요 수, 플레이 수 등 부가 정보 저장
        ProblemInfo info = ProblemInfo.builder()
                .id(saved.getId())
                .difficulty(Difficulty.valueOf(dto.getDifficulty()))
                .likes(0)
                .playCount(0)
                .successCount(0)
                .successRate(0.0)
                .build();

        problemInfoRepository.save(info);

        // 4. 유저-문제 생성 기록 저장
        // 어떤 유저가 어떤 문제를 생성했는지 기록
        UserCreatedProblem createdProblem = UserCreatedProblem.builder()
                .userId(user.getUserId())
                .problemId(saved.getId())
                .createdAt(LocalDateTime.now())
                .build();

        userCreatedProblemRepository.save(createdProblem);

        // 5. 장르 매핑
        for (String genreName : dto.getGenre()) {
            // 장르 엔티티 조회
            Genre genre = genreRepository.findByName(genreName)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장르: " + genreName));

            ProblemGenre problemGenre = ProblemGenre.builder()
                    .problemId(saved.getId())
                    .genreId(genre.getId())
                    .build();

            problemGenreRepository.save(problemGenre);
        }

        return saved;
    }


}
