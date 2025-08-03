package com.ssafy.backend.entity;

import com.ssafy.backend.common.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider")
    private SocialProvider provider;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "total_games")
    private Integer totalGames;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @Column(name = "role", nullable = false)
    private String role;  // "USER", "ADMIN" 등


    /**
     * 새로운 User 인스턴스를 생성합니다.
     *
     * @param socialId 소셜 로그인 ID.
     * @param provider 소셜 로그인 제공자.
     * @param nickname 유저의 닉네임.
     * @param createdAt 유저 계정 생성일.
     * @param deleted 유저 계정 삭제 여부. 계정이 삭제된 경우 `true`, 아니면 `false`.
     * @param role 유저 권한 종류. ADMIN 아니면 USER
     */

    @Builder
    public User(String socialId, SocialProvider provider, String nickname, LocalDateTime createdAt, Boolean deleted,  String role) {
        this.socialId = socialId;
        this.provider = provider;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.totalGames = 0;
        this.wins = 0;
        this.deleted = deleted;
        this.role = role;
    }
}