package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.Difficulty;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class Problem {
    private final String problemId;
    private final String title;
    private final String content;
    private final String answer;
    private final String genre;
    private final Difficulty difficulty;
    private final Long creatorId; // 창작자 (기존 문제는 null)
}