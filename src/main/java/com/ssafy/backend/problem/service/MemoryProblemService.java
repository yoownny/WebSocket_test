package com.ssafy.backend.problem.service;

import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.type.Difficulty;
import com.ssafy.backend.problem.dto.Request.ProblemSubmitRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemCreateResponseDto;
import com.ssafy.backend.repository.memory.impl.MemoryProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryProblemService {

    private final MemoryProblemRepository memoryProblemRepository;

    /**
     * 메모리에 문제 저장 (창작 문제 제출용)
     */
    public ProblemCreateResponseDto saveToMemory(ProblemSubmitRequestDto dto) {
        // 문제 고유 ID 생성 (UUID 사용)
        String uuid = UUID.randomUUID().toString();

        // Problem 객체 생성
        Problem problem = Problem.builder()
                .problemId(uuid)
                .title(dto.getTitle())
                .content(dto.getContent())
                .answer(dto.getAnswer())
                .genre(dto.getGenres())
                .difficulty(Difficulty.valueOf(dto.getDifficulty().toUpperCase()))
                .creatorId(dto.getCreator().getId())
                .nickname(dto.getCreator().getNickname())
                .build();

        // 메모리 저장소에 저장
        memoryProblemRepository.save(problem);

        log.info("메모리 문제 저장 완료: problemId={}, title={}", uuid, dto.getTitle());

        return ProblemCreateResponseDto.builder()
                .problemId(problem.getProblemId())
                .title(problem.getTitle())
                .content(problem.getContent())
                .answer(problem.getAnswer())
                .genres(dto.getGenres())
                .difficulty(dto.getDifficulty())
                .creator(
                        ProblemCreateResponseDto.CreatorInfo.builder()
                                .id(dto.getCreator().getId())
                                .nickname(dto.getCreator().getNickname())
                                .build()
                )
                .createdAt(LocalDateTime.now())
                .storageType(ProblemCreateResponseDto.StorageType.MEMORY)
                .build();
    }

    /**
     * 메모리에서 문제 조회
     */
    public Problem findById(String problemId) {
        return memoryProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 메모리 문제를 찾을 수 없습니다: " + problemId));
    }

    /**
     * 메모리에서 문제 삭제
     */
    public void deleteById(String problemId) {
        memoryProblemRepository.delete(problemId);
        log.info("❌ 메모리 문제 삭제: problemId={}", problemId);
    }

    /**
     * 모든 메모리 문제 조회
     */
    public List<Problem> findAll() {
        List<Problem> problems = memoryProblemRepository.findAll();
        log.debug("전체 메모리 문제 조회: 총 {}개", problems.size());
        return problems;
    }
}