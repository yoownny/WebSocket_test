package com.ssafy.backend.auth.service;

import com.ssafy.backend.common.enums.SocialProvider;
import com.ssafy.backend.config.jwt.JWTUtil;
import com.ssafy.backend.entity.Refresh;
import com.ssafy.backend.entity.User;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.exception.model.ConflictException;
import com.ssafy.backend.repository.RefreshRepository;
import com.ssafy.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshRepository refreshRepository;

    // 토큰 만료 시간 설정
    private long accessExpiration = 24 * 60 * 60 * 1000L; // 24시간
    private long refreshExpiration = 30 * 24 * 60 * 60 * 1000L; // 30일

    /**
     * 닉네임 중복 확인
     */
    public void checkNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new ConflictException(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    /**
     * 회원가입
     */
    public User join(String socialId, String nickname) {
        // 닉네임 중복 체크
        checkNickname(nickname);

        // 소셜 ID 중복 체크
        if (userRepository.existsBySocialId(socialId)) {
            throw new ConflictException(ErrorCode.USER_CONFLICT);
        }

        // User 생성시 Builder 패턴 사용
        User user = User.builder()
                .socialId(socialId)
                .provider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .role("USER")
                .build();

        return userRepository.save(user);
    }

    /**
     * 토큰 생성 및 쿠키 설정을 위한 공통 메서드
     */
    @Transactional
    public HttpHeaders generateAuthTokens(User user, HttpServletResponse response) {
        Long userId = user.getUserId();
        String nickname = user.getNickname();
        String role = user.getRole();

        // JWT 생성
        String accessToken = jwtUtil.createJwt("access", userId, nickname, role, accessExpiration);
        String refreshToken = jwtUtil.createJwt("refresh", userId, nickname, role, refreshExpiration);

        // DB에 리프레시 토큰 저장
        saveRefreshToken(userId, refreshToken);

        // Access Token → Authorization 헤더로 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // Refresh Token → HttpOnly 쿠키로 설정
        Cookie refreshCookie = new Cookie("refresh", refreshToken);
        refreshCookie.setMaxAge((int) (refreshExpiration / 1000)); // 30일
        refreshCookie.setPath("/api/auth");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return headers;
    }

    /**
     * 리프레시 토큰을 DB에 저장하는 메서드
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        // 만료 시간 계산
        LocalDateTime expiryDate = LocalDateTime.ofInstant(
                Instant.now().plusMillis(refreshExpiration),
                ZoneId.systemDefault()
        );

        // 기존 토큰이 있는지 확인하고, 있으면 업데이트. 없으면 새로 생성
        Refresh refreshEntity = refreshRepository.findById(userId)
                .orElse(new Refresh());

        // 리프레시 토큰 정보 설정
        refreshEntity.setUserId(userId);
        refreshEntity.setRefresh(refreshToken);
        refreshEntity.setExpiryDate(expiryDate);

        // 저장
        refreshRepository.save(refreshEntity);
    }

    /**
     * 쿠키에서 refresh 토큰 추출
     */
    public String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 리프레시 토큰 유효성 검증
     */
    public boolean validateRefreshToken(String refreshToken) {
        // refresh가 DB에 존재하는지 확인
        Optional<Refresh> refreshOpt = refreshRepository.findByRefresh(refreshToken);
        if (refreshOpt.isEmpty()) {
            return false;
        }

        // JWT 토큰 자체 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            return false;
        }

        // 토큰 유형 확인
        if (!"refresh".equals(jwtUtil.getCategory(refreshToken))) {
            return false;
        }

        // DB 저장된 토큰 만료 시간 확인
        Refresh refresh = refreshOpt.get();
        return LocalDateTime.now().isBefore(refresh.getExpiryDate());
    }

    /**
     * refresh 토큰으로부터 사용자 ID를 조회
     */
    public Long getUserIdByRefreshToken(String refreshToken) {
        return refreshRepository.findByRefresh(refreshToken)
                .map(Refresh::getUserId)
                .orElse(-1L);
    }

    /**
     * logout - 리프레시 토큰 삭제
     */
    @Transactional
    public void logout(Long userId) {
        refreshRepository.deleteById(userId);
    }
}