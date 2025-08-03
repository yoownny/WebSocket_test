package com.ssafy.backend.problem.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ProblemCreateDto {

    private String title;
    private String content;
    private String answer;
    private List<String> genre;
    private String difficulty;
    private CreatorDto creator;
}
