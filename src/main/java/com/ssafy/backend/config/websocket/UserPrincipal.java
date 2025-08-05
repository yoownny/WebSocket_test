package com.ssafy.backend.config.websocket;

import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private final Long userId;
    private final String nickname;
    private final String role;

    @Override
    public String getName() {
        return String.valueOf(userId); // convertAndSendToUser에서 사용
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId=" + userId + ", nickname='" + nickname + "'}";
    }
}

