package com.ssafy.backend.config.security;

import com.ssafy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security의 AuthenticationManager가 사용자 인증 시 이 서비스를 호출
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 사용자명(socialId)으로 사용자 정보 조회
     * Spring Security가 인증 과정에서 호출하는 메서드
     */
    @Override
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public UserDetails loadUserByUsername(String socialId) throws UsernameNotFoundException {
        // DB에서 socialId로 사용자 조회
        return userRepository.findBySocialId(socialId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다: " + socialId));
    }
}