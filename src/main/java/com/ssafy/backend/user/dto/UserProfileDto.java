package com.ssafy.backend.user.dto;

import com.ssafy.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 사용자 프로필 조회 응답 DTO (공개 정보만)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    /** 사용자 ID */
    private Long userId;

    /** 닉네임 */
    private String nickname;

    /** 가입일시 */
    private LocalDateTime createdAt;

    /** 총 게임 참여 횟수 */
    private Integer totalGames;

    /** 정답 맞춘 횟수 */
    private Integer wins;

    /** 정답률 (백분율) */
    private Double winRate;

    /**
     * User 엔티티 -> UserProfileDto로 변환
     */
    public static UserProfileDto from(User user) {
        // 정답률 계산 (0으로 나누는 것 방지)
        double winRate = 0.0;
        if (user.getTotalGames() != null && user.getTotalGames() > 0 && user.getWins() != null) {
            winRate = (double) user.getWins() / user.getTotalGames() * 100;
            winRate = Math.round(winRate * 10.0) / 10.0; // 소수점 첫째 자리까지
        }

        return new UserProfileDto(
                user.getUserId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getTotalGames() != null ? user.getTotalGames() : 0,
                user.getWins() != null ? user.getWins() : 0,
                winRate
        );
    }
}