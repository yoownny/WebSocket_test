package com.ssafy.backend.repository.memory.api;

import java.util.Optional;

public interface UserRoomRepository {
    void mapUserToRoom(long userId, long roomId);

    Optional<Long> findRoomByUserId(long userId);

    void removeUserMapping(long userId);

    void clearStore();
}