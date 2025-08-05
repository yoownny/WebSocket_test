package com.ssafy.backend.problem.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemCreateResponseDto {

    // DB 저장일 경우: Long 타입이지만 문자열로 변환
    // 메모리 저장일 경우: UUID 기반의 String ID
    private String problemId;
    private String title;
    private String content;
    private String answer;
    private List<String> genres;
    private String difficulty;
    private CreatorInfo creator;
    private LocalDateTime createdAt;
    private StorageType storageType;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private Long id;
        private String nickname;
    }

    public enum StorageType {
        MEMORY("메모리 임시 저장"),
        DATABASE("데이터베이스 영구 저장");

        private final String description;

        StorageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
