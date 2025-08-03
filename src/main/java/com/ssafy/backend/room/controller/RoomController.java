package com.ssafy.backend.room.controller;

import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.room.dto.request.RoomCreateRequest;
import com.ssafy.backend.room.dto.request.RoomJoinRequest;
import com.ssafy.backend.room.dto.request.RoomLeaveRequest;
import com.ssafy.backend.room.dto.response.RoomListResponse;
import com.ssafy.backend.room.dto.response.RoomResponse;
import com.ssafy.backend.room.service.RoomService;
import com.ssafy.backend.websocket.service.WebSocketNotificationService;
import com.ssafy.backend.websocket.util.WebSocketUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final WebSocketNotificationService webSocketNotificationService;

    // 방 생성
    @MessageMapping("/room/create")
    public void createRoom(@Payload RoomCreateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            Room room = roomService.createRoom(request.getTitle(), request.getMaxPlayers(),
                    request.getTimeLimit(), userId, nickname);

            RoomResponse response = RoomResponse.from(room);
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_CREATED", response);

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방 입장
    @MessageMapping("/room/join")
    public void joinRoom(@Payload RoomJoinRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            Room room = roomService.joinRoom(request.getRoomId(), userId, nickname);

            RoomResponse response = RoomResponse.from(room);

            // 본인에게 입장 성공 알림
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_JOINED", response);

            // 방의 모든 사람들에게 새 참가자 알림
            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "PLAYER_JOINED", response);

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방 퇴장
    @MessageMapping("/room/leave")
    public void leaveRoom(@Payload RoomLeaveRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            // 방에 있는 사람들에게 퇴장 알림 (퇴장 전에 먼저)
            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "PLAYER_LEFT", userId);
            roomService.leaveRoom(request.getRoomId(), userId);

            // (방장 변경 알림 추가)

            // 본인에게 퇴장 완료 알림
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_LEFT", "방에서 나왔습니다.");

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방 목록 조회
    @MessageMapping("/room/list")
    public void getRoomList(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            RoomListResponse response = RoomListResponse.from(roomService.getAllRooms());
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_LIST", response);
        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }
}
