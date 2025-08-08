package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class TransferHostRequest {
    private Long roomId;
    private Long targetUserId;
}
