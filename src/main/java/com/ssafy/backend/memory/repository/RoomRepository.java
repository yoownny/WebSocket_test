package com.ssafy.backend.memory.repository;

import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.type.RoomState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class RoomRepository {
    private static final int MAX_ROOMS = 1000;
    // 방 저장 (방ID, 방 객체)
    private final ConcurrentHashMap<Long, Room> rooms = new ConcurrentHashMap<>();
    // 사용 가능한 방ID
    private final ConcurrentLinkedQueue<Long> availableIds = new ConcurrentLinkedQueue<>();

    public RoomRepository() {
        // 방ID를 사용 가능한 ID 풀에 추가
        for (long i = 0; i < MAX_ROOMS; i++) {
            availableIds.offer(i);
        }
    }

    // 사용 가능한 방ID 반환
    public Long getNextRoomId() {
        Long roomId = availableIds.poll();
        if (roomId == null) {
            throw new RuntimeException("사용 가능한 방이 없습니다.");
        }
        return roomId;
    }

    // 방 정보 저장
    public Room save(Room room) {
        rooms.put(room.getRoomId(), room);
        return room;
    }

    // 방ID로 특정 방 조회
    public Room findById(Long roomId) {
        return rooms.get(roomId);
    }

    // 저장된 모든 방 목록 조회
    public List<Room> findAll() {
        return new ArrayList<>(rooms.values());
    }

    // 방ID를 삭제하고 ID를 재사용 풀에 반환
    public void delete(Long roomId) {
        Room removed = rooms.remove(roomId);
        if (removed != null) {
            availableIds.offer(roomId);
        }
    }

    // 사용자ID - 방ID
    private final ConcurrentHashMap<Long, Long> userToRoom = new ConcurrentHashMap<>();

    // 사용자를 특정 방에 매핑
    public void setUserRoom(Long userId, Long roomId) {
        userToRoom.put(userId, roomId);
    }

    // 사용자가 현재 참여하고 있는 방ID 반환
    public Long getCurrentRoom(Long userId) {
        return userToRoom.get(userId);
    }

    // 사용자의 방 매핑 정보 제거
    public void removeUserRoom(Long userId) {
        userToRoom.remove(userId);
    }

    public List<Room> findAllSorted() {
        return rooms.values().stream()
                .sorted((r1, r2) -> {
                    // 상태별 정렬 후 방 번호 정렬
                    int stateCompare = r1.getState().compareTo(r2.getState());
                    if (stateCompare != 0) return stateCompare;
                    return r1.getRoomId().compareTo(r2.getRoomId());
                })
                .collect(Collectors.toList());
    }

    public List<Room> findByState(RoomState state) {
        return rooms.values().stream()
                .filter(room -> room.getState() == state)
                .sorted((r1, r2) -> r1.getRoomId().compareTo(r2.getRoomId()))
                .collect(Collectors.toList());
    }
}
