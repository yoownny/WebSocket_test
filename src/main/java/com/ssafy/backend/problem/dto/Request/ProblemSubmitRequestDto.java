package com.ssafy.backend.problem.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSubmitRequestDto {

    @NotBlank(message = "문제 제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "문제 내용은 필수입니다")
    @Size(max = 2000, message = "내용은 2000자를 초과할 수 없습니다")
    private String content;

    @NotBlank(message = "정답은 필수입니다")
    @Size(max = 500, message = "정답은 500자를 초과할 수 없습니다")
    private String answer;

    @NotEmpty(message = "장르는 최소 1개 이상 선택해야 합니다")
    @Size(max = 3, message = "장르는 최대 3개까지 선택 가능합니다")
    private List<String> genres;

    @NotBlank(message = "난이도는 필수입니다")
    private String difficulty; // EASY, MEDIUM, HARD

    @NotNull(message = "생성자 정보는 필수입니다")
    private CreatorDto creator;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreatorDto {
        @NotNull(message = "생성자 ID는 필수입니다")
        private Long id;

        @NotBlank(message = "생성자 닉네임은 필수입니다")
        @Size(max = 20, message = "닉네임은 20자를 초과할 수 없습니다")
        private String nickname;
    }
}