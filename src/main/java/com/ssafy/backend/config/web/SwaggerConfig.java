package com.ssafy.backend.config.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info (
                title = "TDC API",
                description ="거북탐정과 사건파일 API 명세서",
                version = "v1"
        )
)

@Configuration // 설정 파일
public class SwaggerConfig {

    @Bean
    GroupedOpenApi groupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("All API") // 그룹명
                .pathsToMatch("/**") // 모든 경로와 매칭
                .build();
    }
}
