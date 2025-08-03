package com.ssafy.backend.room.service;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.RoomState;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService { 
    private final RoomRepository roomRepository;

    // 방 생성
    public Room createRoom(String title, int maxPlayers, int timeLimit, Long userId, String nickname) {
        // 방 생성
        Long roomId = roomRepository.getNextRoomId();
        Room room = new Room(roomId, maxPlayers, timeLimit);
        room.setTitle(title);
        room.setState(RoomState.WAITING);

        // 방장으로 입장
        Player host = new Player(userId, nickname);
        host.setRole(PlayerRole.HOST);
        host.setState(PlayerState.READY);

        room.getPlayers().put(userId, host);
        room.getPlayerOrder().add(userId);
        room.setHostId(userId);

        // 저장
        roomRepository.save(room);
        roomRepository.setUserRoom(userId, roomId);

        return room;
    }

    // 방 입장
    public Room joinRoom(Long roomId, Long userId, String nickname) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }
        if (!room.canJoin()) {
            throw new RuntimeException("입장할 수 없는 방입니다.");
        }
        if (room.hasPlayer(userId)) {
            throw new RuntimeException("이미 참가한 방입니다.");
        }

        // 이미 다른 방에 있다면 퇴장
        Long currentRoomId = roomRepository.getCurrentRoom(userId);
        if (currentRoomId != null && !currentRoomId.equals(roomId)) {
            leaveRoom(currentRoomId, userId);
        }

        // 플레이어 추가
        Player player = new Player(userId, nickname);
        player.setRole(PlayerRole.PARTICIPANT);
        player.setState(PlayerState.READY);

        room.getPlayers().put(userId, player);
        room.getPlayerOrder().add(userId);

        roomRepository.save(room);
        roomRepository.setUserRoom(userId, roomId);

        return room;
    }

    // 방 퇴장
    public void leaveRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId);
        if (room == null || !room.hasPlayer(userId)) {
            return; // 이미 없으면 성공으로 처리
        }

        // 플레이어 제거
        room.getPlayers().remove(userId);
        room.getPlayerOrder().remove(userId);
        roomRepository.removeUserRoom(userId);

        // 방장이 나갔으면 방장 이양
        if (userId.equals(room.getHostId())) {
            if (room.isEmpty()) {
                room.setHostId(null);
            } else {
                // 첫 번째 남은 플레이어를 방장으로
                Long newHostId = room.getPlayerOrder().get(0);
                Player newHost = room.getPlayer(newHostId);
                newHost.setRole(PlayerRole.HOST);
                room.setHostId(newHostId);
            }
        }

        // 방이 비었으면 삭제
        if (room.isEmpty()) {
            roomRepository.delete(roomId);
        } else {
            roomRepository.save(room);
        }

    }

    // 방 목록 조회
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}
