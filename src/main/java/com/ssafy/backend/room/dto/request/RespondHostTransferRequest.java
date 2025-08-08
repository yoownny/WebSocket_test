package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class RespondHostTransferRequest {
    private Long roomId;
    private boolean accept; // true: 수락, false: 거절
}
