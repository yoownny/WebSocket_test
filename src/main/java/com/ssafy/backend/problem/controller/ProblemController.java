package com.ssafy.backend.problem.controller;

import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.entity.Problem;
import com.ssafy.backend.problem.dto.ProblemCreateDto;
import com.ssafy.backend.problem.dto.ProblemCreateResponseDto;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemListResponseDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping("/search")
    public ProblemListResponseDto searchProblems(@ModelAttribute ProblemSearchRequestDto requestDto) {
        Slice<ProblemSummaryDto> slice = problemService.searchProblems(requestDto);

        Long nextCursor = slice.hasContent() ?
                Long.parseLong(slice.getContent().get(slice.getContent().size() - 1).getProblemId()) : null;

        return ProblemListResponseDto.builder()
                .problemList(slice.getContent())
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();
    }


    @PostMapping("/custom")
    public ResponseEntity<?> createProblem(@RequestBody ProblemCreateDto dto) {
        Problem saved = problemService.create(dto);

        // 저장된 문제 정보를 바탕으로 응답 DTO 구성
        ProblemCreateResponseDto response = ProblemCreateResponseDto.builder()
                .title(saved.getTitle())
                .content(saved.getContent())
                .answer(saved.getAnswer())
                .genre(dto.getGenre())
                .difficulty(dto.getDifficulty())
                .creator(dto.getCreator())
                .build();

        return ApiResponse.success(
                HttpStatus.CREATED,
                "창작 문제가 성공적으로 생성되었습니다",
                response
        );
    }
}
