package com.ssafy.backend.user.service;

import com.ssafy.backend.entity.User;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.exception.model.NotFoundException;
import com.ssafy.backend.repository.UserRepository;
import com.ssafy.backend.user.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 공개 프로필 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 공개 프로필 정보
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 조회 시도 - userId: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND);
                });

        // 삭제된 사용자 체크
        if (user.getDeleted()) {
            log.warn("삭제된 사용자 프로필 조회 시도 - userId: {}", userId);
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("사용자 공개 프로필 조회 성공 - userId: {}, nickname: {}", userId, user.getNickname());
        return UserProfileDto.from(user);
    }
}