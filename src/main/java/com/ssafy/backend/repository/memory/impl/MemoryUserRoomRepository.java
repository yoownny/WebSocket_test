package com.ssafy.backend.repository.memory.impl;

import com.ssafy.backend.repository.memory.api.UserRoomRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryUserRoomRepository implements UserRoomRepository {

    // Key 타입을 String에서 Long으로 변경
    private final ConcurrentHashMap<Long, Long> userToRoom = new ConcurrentHashMap<>();

    @Override
    public void mapUserToRoom(long userId, long roomId) {
        userToRoom.put(userId, roomId);
    }

    @Override
    public Optional<Long> findRoomByUserId(long userId) {
        return Optional.ofNullable(userToRoom.get(userId));
    }

    @Override
    public void removeUserMapping(long userId) {
        userToRoom.remove(userId);
    }

    @Override
    public void clearStore() {
        userToRoom.clear();
    }
}