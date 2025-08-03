package com.ssafy.backend.problem.dto.Request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProblemSearchRequestDto {

    private List<String> genre = List.of();
    private String difficulty;
    private String source;
    private String sort = "latest";
    private String keyword;
    private Long cursor;
    private Integer size = 10;
}
