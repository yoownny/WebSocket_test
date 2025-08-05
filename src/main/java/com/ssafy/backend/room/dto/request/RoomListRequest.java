package com.ssafy.backend.room.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomListRequest {
    private Integer page = 0; // 페이지 번호 (기본값: 0)
    private Integer size = 20; // 페이지 크기 (기본값: 20)
    private String state;

    // 유효성 검사
    public Integer getValidatedSize() {
        if (size == null || size <= 0) return 20;
        if (size > 50) return 50;  // 최대 50개로 제한
        return size;
    }

    public Integer getValidatedPage() {
        if (page == null || page < 0) return 0;
        return page;
    }
}
