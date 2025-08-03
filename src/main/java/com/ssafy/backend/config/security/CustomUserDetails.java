package com.ssafy.backend.config.security;

import com.ssafy.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Spring Security가 사용자 정보를 이해할 수 있게 해주는 어댑터 클래스
 * 우리의 User 엔티티를 Spring Security의 UserDetails 인터페이스에 맞게 변환
 */
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User user;

    /**
     * JWT 필터나 Controller에서 실제 User 정보에 접근할 때 사용
     */
    public User getUser() {
        return user;
    }

    /**
     * 사용자 권한 목록 반환
     * Spring Security가 인가(Authorization) 처리할 때 사용
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // 사용자 역할에 ROLE_ 접두사 추가
        String role = "ROLE_" + user.getRole();

        // 역할을 목록에 추가
        authorities.add(new SimpleGrantedAuthority(role));

        return authorities;
    }


    /**
     * JWT 필터에서 사용자 ID가 필요할 때 사용
     */
    public Long getUserId() {
        return user.getUserId();
    }

    /**
     * 소셜 로그인을 사용하므로 socialId를 username으로 사용
     */
    @Override
    public String getUsername() {
        return user.getSocialId();
    }

    /**
     * 소셜 로그인은 외부 제공자가 인증을 담당하므로 더미 값 반환
     */
    @Override
    public String getPassword() {
        return "{noop}";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !user.getDeleted();
    }
}