package com.ssafy.backend.problem.controller;

import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.common.response.SuccessResponse;
import com.ssafy.backend.config.security.CustomUserDetails;
import com.ssafy.backend.entity.Problem;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.exception.SuccessCode;
import com.ssafy.backend.problem.dto.Request.ProblemCreateDto;
import com.ssafy.backend.problem.dto.Request.ProblemEvaluateRequestDto;
import com.ssafy.backend.problem.dto.Request.ProblemSubmitRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemCreateResponseDto;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemDetailResponseDto;
import com.ssafy.backend.problem.dto.Response.ProblemListResponseDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.problem.service.MemoryProblemService;
import com.ssafy.backend.problem.service.ProblemEvaluateService;
import com.ssafy.backend.problem.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final MemoryProblemService memoryProblemService;
    private final ProblemService problemService;
    private final ProblemEvaluateService problemEvaluateService;


     // 메모리에 문제 임시 저장
    @PostMapping("/memory")
    public ResponseEntity<SuccessResponse<ProblemCreateResponseDto>> createMemoryProblem(
            @Valid @RequestBody ProblemSubmitRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        ProblemCreateResponseDto response = memoryProblemService.saveToMemory(dto);

        return ApiResponse.success(
                SuccessCode.CREATE_SUCCESS.getStatus(),
                SuccessCode.CREATE_SUCCESS.getMessage(),
                response
        );
    }

    // 문제 평가
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(
            @Valid @RequestBody ProblemEvaluateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        if (!request.isValidRequest()) {
            return ApiResponse.error(ErrorCode.INVALID_REQUEST_BODY);
        }

        try {
            boolean saved = problemEvaluateService.evaluate(request, userId);

            if (saved) {
                return ApiResponse.success(SuccessCode.CREATE_SUCCESS.getStatus(), SuccessCode.CREATE_SUCCESS.getMessage());
            } else {
                return ApiResponse.success(SuccessCode.UPDATE_SUCCESS.getStatus(), SuccessCode.UPDATE_SUCCESS.getMessage());
            }
        } catch (Exception e) {
            log.error("평가 처리 중 오류 발생", e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 문제 검색 (커서 기반 무한스크롤)
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

    // 창작 문제를 DB에 직접 저장
    @PostMapping("/custom")
    public ResponseEntity<?> createProblem(
            @Valid @RequestBody ProblemCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        Problem saved = problemService.create(dto);

        ProblemCreateResponseDto response = ProblemCreateResponseDto.builder()
                .problemId(saved.getId().toString())
                .title(saved.getTitle())
                .content(saved.getContent())
                .answer(saved.getAnswer())
                .genres(dto.getGenres())
                .difficulty(dto.getDifficulty())
                .creator(
                        ProblemCreateResponseDto.CreatorInfo.builder()
                                .id(dto.getCreator().getId())
                                .nickname(dto.getCreator().getNickname())
                                .build()
                )
                .createdAt(LocalDateTime.now())
                .storageType(ProblemCreateResponseDto.StorageType.DATABASE)
                .build();

        log.info("창작 문제 DB 저장 완료: problemId={}, userId={}", saved.getId(), userId);

        return ApiResponse.success(
                SuccessCode.CREATE_SUCCESS.getStatus(),
                SuccessCode.CREATE_SUCCESS.getMessage(),
                response
        );
    }

    @GetMapping("/{problemId}")
    public ResponseEntity<?> getProblemDetail(
            @PathVariable("problemId") Long problemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            ProblemDetailResponseDto response = problemService.getProblemDetail(problemId);

            return ApiResponse.success(
                    SuccessCode.GET_SUCCESS.getStatus(),
                    SuccessCode.GET_SUCCESS.getMessage(),
                    response
            );
        } catch (IllegalArgumentException e) {
            log.error("문제 조회 실패: problemId={}, error={}", problemId, e.getMessage());
            return ApiResponse.error(ErrorCode.INVALID_REQUEST_BODY);
        } catch (Exception e) {
            log.error("문제 상세 조회 중 오류 발생: problemId={}", problemId, e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}