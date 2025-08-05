package com.ssafy.backend.room.controller;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.room.dto.request.RoomCreateRequest;
import com.ssafy.backend.room.dto.request.RoomJoinRequest;
import com.ssafy.backend.room.dto.request.RoomLeaveRequest;
import com.ssafy.backend.room.dto.request.RoomListRequest;
import com.ssafy.backend.room.dto.response.*;
import com.ssafy.backend.room.service.RoomService;
import com.ssafy.backend.websocket.service.WebSocketNotificationService;
import com.ssafy.backend.websocket.util.WebSocketUtils;
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
    private final RoomRepository roomRepository;

    // 방 생성
    @MessageMapping("/room/create")
    public void createRoom(@Payload RoomCreateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            Room room = roomService.createRoom(request.getMaxPlayers(), request.getTimeLimit(), userId, nickname, request.getProblemInfo());

            // 방장에게는 정답 포함해서 전송
            RoomResponse hostResponse = RoomResponse.from(room, true);
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_CREATED", hostResponse);

            // 로비에는 정답 제외하고 전송
            RoomResponse lobbyResponse = RoomResponse.from(room, false);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_CREATED", lobbyResponse);
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
            // 결과 DTO로 모든 정보를 한 번에 받음
            JoinRoomResult result = roomService.joinRoom(request.getRoomId(), userId, nickname);

            // 본인에게 입장 성공 알림 (방장이면 정답 포함)
            RoomResponse userResponse = RoomResponse.from(result.getRoom(), result.isHost());
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_JOINED", userResponse);

            // 방의 다른 사람들에게는 새 참가자 정보만 전송
            PlayerResponse newPlayerResponse = PlayerResponse.from(result.getJoinedPlayer());
            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "PLAYER_JOINED", newPlayerResponse);

            // 로비 업데이트 (정답 제외)
            RoomResponse lobbyResponse = RoomResponse.from(result.getRoom(), false);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_UPDATED", lobbyResponse);

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방 퇴장
    @MessageMapping("/room/leave")
    public void leaveRoom(@Payload RoomLeaveRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            // 나간 참가자 ID만 전송
            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "PLAYER_LEAVING", userId);

            // 결과 DTO로 모든 상태 변화를 한 번에 받음
            LeaveRoomResult result = roomService.leaveRoom(request.getRoomId(), userId);

            // 결과에 따라 적절한 알림 전송
            Long leavingUserId = result.getLeavingUserId();

            // 본인에게 퇴장 완료 알림
            webSocketNotificationService.sendToUser(leavingUserId, "/queue/room", "ROOM_LEFT", "방에서 나왔습니다.");

            if (result.isRoomDeleted()) {
                // 방이 삭제된 경우
                log.info("방이 삭제되었습니다: roomId={}, lastUser={}", request.getRoomId(), leavingUserId);
            } else if (result.isHostChanged()) { // 방장이 변경된 경우
                // 룸 업데이트
                PlayerResponse newHostResponse = PlayerResponse.from(result.getNewHost());
                webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "HOST_CHANGED", newHostResponse);

                // 로비 업데이트 (정답 제외)
                RoomResponse lobbyResponse = RoomResponse.from(result.getRoom(), false);
                webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_UPDATED", lobbyResponse);

                log.info("방장이 변경되었습니다: roomId={}, oldHost={}, newHost={}",
                        request.getRoomId(), leavingUserId, result.getNewHost().getUserId());

            } else { // 일반 참가자가 나간 경우
                // 로비 업데이트 (정답 제외)
                RoomResponse lobbyResponse = RoomResponse.from(result.getRoom(), false);
                webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_UPDATED", lobbyResponse);

                log.info("참가자가 퇴장했습니다: roomId={}, userId={}", request.getRoomId(), leavingUserId);
            }

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방 목록 조회
    @MessageMapping("/room/list")
    public void getRoomList(@Payload RoomListRequest roomListRequest, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            // 요청이 null인 경우 기본값 사용
            if (roomListRequest == null) {
                roomListRequest = new RoomListRequest();
            }

            RoomListResponse response = roomService.getRooms(roomListRequest);
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_LIST", response);

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }
}
