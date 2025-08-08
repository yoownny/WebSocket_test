package com.ssafy.backend.room.controller;

import com.ssafy.backend.memory.Room;
import com.ssafy.backend.room.dto.request.*;
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

import java.util.Map;
import java.util.stream.Collectors;

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
            Room room = roomService.createRoom(request.getMaxPlayers(), request.getTimeLimit(), userId, nickname, request.getProblemInfo());

            // 방장에게는 정답 포함해서 전송
            RoomResponse hostResponse = RoomResponse.from(room, true);
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_CREATED", hostResponse);

            // 로비
            RoomListRequest roomListRequest = new RoomListRequest();
            RoomListResponse response = roomService.getRooms(roomListRequest);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);
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

            // 로비
            RoomListRequest roomListRequest = new RoomListRequest();
            RoomListResponse response = roomService.getRooms(roomListRequest);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);

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

                // 로비
                RoomListRequest roomListRequest = new RoomListRequest();
                RoomListResponse response = roomService.getRooms(roomListRequest);
                webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);

                log.info("방장이 변경되었습니다: roomId={}, oldHost={}, newHost={}", request.getRoomId(), leavingUserId, result.getNewHost().getUserId());

            } else { // 일반 참가자가 나간 경우
                // 로비
                RoomListRequest roomListRequest = new RoomListRequest();
                RoomListResponse response = roomService.getRooms(roomListRequest);
                webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);

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

    // 방 설정 변경
    @MessageMapping("/room/settings")
    public void updateRoomSettings(@Payload RoomSettingsRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            RoomSettingsUpdateResult result = roomService.updateRoomSettings(request.getRoomId(), userId, nickname, request.getMaxPlayers(), request.getTimeLimit());

            // 본인(방장)에게 설정 변경 성공 알림
            Map<String, Object> successResponse = Map.of("success", true, "message", "방 설정이 변경되었습니다.", "updatedSettings", result.getUpdatedSettings());
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ROOM_SETTINGS_UPDATED", successResponse);

            // 방 전체에 설정 변경 알림 (방장 포함 모든 사람)
            Room updatedRoom = result.getRoom();

            // 방 전체 브로드캐스트용 응답 데이터
            Map<String, Object> broadcastResponse = Map.of(
                    "roomId", updatedRoom.getRoomId(),
                    "title", "게임방", // 기본 제목 또는 실제 방 제목으로 변경
                    "maxPlayers", updatedRoom.getMaxPlayers(),
                    "currentPlayers", updatedRoom.getCurrentPlayerCount(),
                    "timeLimit", updatedRoom.getTimeLimit(),
                    "hostId", updatedRoom.getHostId(),
                    "changedBy", Map.of("userId", result.getChangedBy(), "nickname", result.getChangedByNickname()),
                    "changedAt", result.getChangedAt(),
                    "players", updatedRoom.getPlayers().values().stream()
                            .map(PlayerResponse::from)
                            .collect(Collectors.toList())
            );

            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "ROOM_SETTINGS_CHANGED", broadcastResponse);

            // 로비
            RoomListRequest roomListRequest = new RoomListRequest();
            RoomListResponse response = roomService.getRooms(roomListRequest);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);

            log.info("방 설정 변경 완료: roomId={}, userId={}, maxPlayers={}, timeLimit={}",
                    request.getRoomId(), userId, request.getMaxPlayers(), request.getTimeLimit());

        } catch (Exception e) {
            log.error("방 설정 변경 실패: roomId={}, userId={}, error={}", request.getRoomId(), userId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방장 권한 넘기기 요청
    @MessageMapping("/room/transfer-host")
    public void transferHost(@Payload TransferHostRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            TransferHostRequestResult result = roomService.requestHostTransfer(request.getRoomId(), userId, request.getTargetUserId());

            // 대상자에게 권한 요청 알림
            webSocketNotificationService.sendToUser(request.getTargetUserId(), "/queue/room", "HOST_TRANSFER_REQUEST",
                    Map.of("requesterId", userId, "requesterNickname", nickname, "roomId", request.getRoomId()));

            // 방장에게 요청 전송 완료 알림
            webSocketNotificationService.sendToUser(userId, "/queue/room", "HOST_TRANSFER_SENT",
                    Map.of("targetUserId", request.getTargetUserId(), "targetNickname", result.getTarget().getNickname(), "message", "권한 넘기기 요청을 전송했습니다."));

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 방장 권한 넘기기 응답
    @MessageMapping("/room/respond-host-transfer")
    public void respondHostTransfer(@Payload RespondHostTransferRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            TransferHostResponseResult result = roomService.respondToHostTransfer(request.getRoomId(), userId, request.isAccept());

            if (result.isAccepted()) {
                // 새 방장에게는 정답 포함된 방 정보 개별 전송
                RoomResponse hostResponse = RoomResponse.from(result.getRoom(), true);
                webSocketNotificationService.sendToUser(result.getNewHost().getUserId(), "/queue/room", "ROOM_UPDATED", hostResponse);

                // 방 전체에는 정답 제외하고 권한 변경 알림
                RoomResponse roomResponse = RoomResponse.from(result.getRoom(), false);
                webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "HOST_CHANGED",
                        Map.of(
                                "roomId", request.getRoomId(),
                                "oldHostId", result.getOldHost().getUserId(),
                                "newHostId", result.getNewHost().getUserId(),
                                "newHostNickname", result.getNewHost().getNickname(),
                                "room", roomResponse
                        )
                );
            } else {
                // 거절한 경우 - 기존 방장에게만 알림
                webSocketNotificationService.sendToUser(result.getOldHost().getUserId(), "/queue/room", "HOST_TRANSFER_DECLINED",
                        Map.of(
                                "targetUserId", userId,
                                "targetNickname", result.getNewHost().getNickname(),
                                "message", "권한 넘기기 요청이 거절되었습니다."
                        )
                );
            }

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 문제 정보 업데이트
    @MessageMapping("/room/problem/update")
    public void updateRoomProblem(@Payload RoomProblemUpdateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
        String nickname = WebSocketUtils.getNicknameFromSession(headerAccessor);

        try {
            // 서비스에서 문제 변경 처리
            ProblemUpdateResult result = roomService.updateRoomProblem(request, userId, nickname);

            // 방장에게 변경 결과 알림 (정답 포함)
            ProblemUpdateSuccessResponse hostResponse = ProblemUpdateSuccessResponse.from(result.getUpdatedProblem(), true);
            webSocketNotificationService.sendToUser(userId, "/queue/room", "PROBLEM_UPDATE_SUCCESS", hostResponse);

            // 방에 있는 다른 참가자들에게 알림 (정답 숨김)
            ProblemUpdateSuccessResponse participantResponse = ProblemUpdateSuccessResponse.from(result.getUpdatedProblem(), false);
            webSocketNotificationService.sendToTopic("/topic/room/" + result.getRoom().getRoomId(), "ROOM_PROBLEM_UPDATED", participantResponse);

            // 로비
            RoomListRequest roomListRequest = new RoomListRequest();
            RoomListResponse response = roomService.getRooms(roomListRequest);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_LIST", response);

            log.info("방 문제 변경 완료: roomId={}, userId={}, problemId={}, problemType={}",
                    result.getRoom().getRoomId(), userId, request.getProblemId(), request.getProblemType());

        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

    // 준비 상태 변경
    @MessageMapping("/room/ready")
    public void changeReadyState(@Payload ReadyStateChangeRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = WebSocketUtils.getUserIdFromSession(headerAccessor);

        try {
            ReadyStateChangeResult result = roomService.changeReadyState(request.getRoomId(), userId, request.getReadyState());

            // 본인에게 준비 상태 변경 결과 알림
            ReadyStateChangeResult.PersonalStatusChange personalResponse =
                    ReadyStateChangeResult.PersonalStatusChange.builder()
                            .success(true)
                            .userId(userId)
                            .newState(result.getNewReadyState())
                            .message(result.getNewReadyState().getDescription() + "로 변경되었습니다.")
                            .build();

            webSocketNotificationService.sendToUser(userId, "/queue/room", "READY_STATUS_CHANGED", personalResponse);

            // 방의 모든 참가자에게 준비 상태 업데이트 브로드캐스트
            ReadyStateChangeResult.RoomReadyStateUpdate broadcastResponse =
                    ReadyStateChangeResult.RoomReadyStateUpdate.from(result.getRoom(), result.isAllReady(), result.isCanStartGame());

            webSocketNotificationService.sendToTopic("/topic/room/" + request.getRoomId(), "ROOM_READY_STATUS_UPDATED", broadcastResponse);

            log.info("준비 상태 변경 완료: roomId={}, userId={}, newReadyState={}, allReady={}",
                    request.getRoomId(), userId, result.getNewReadyState(), result.isAllReady());

        } catch (Exception e) {
            log.error("준비 상태 변경 실패: roomId={}, userId={}, error={}", request.getRoomId(), userId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/room", "ERROR", e.getMessage());
        }
    }

}
