package com.ssafy.backend.room.service;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.ReadyState;
import com.ssafy.backend.memory.type.RoomState;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.room.dto.request.RoomProblemUpdateRequest;
import com.ssafy.backend.room.dto.response.*;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.problem.service.MemoryProblemService;
import com.ssafy.backend.repository.ProblemRepositoryCustom;
import com.ssafy.backend.room.dto.request.RoomCreateRequest;
import com.ssafy.backend.room.dto.request.RoomListRequest;
import com.ssafy.backend.websocket.service.WebSocketNotificationService;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final MemoryProblemService memoryProblemService;
    private final ProblemRepositoryCustom problemRepositoryCustom;

    // 방 생성
    public Room createRoom(int maxPlayers, int timeLimit, Long userId, String nickname, RoomCreateRequest.ProblemInfo problemInfo) {
        // 이미 다른 방에 참여 중인지 확인
        Long currentRoomId = roomRepository.getCurrentRoom(userId);
        if (currentRoomId != null) {
            leaveRoom(currentRoomId, userId); // 기존 방에서 나가기
        }

        // 방 생성
        Long roomId = roomRepository.getNextRoomId();
        Room room = new Room(roomId, maxPlayers, timeLimit);
        room.setState(RoomState.WAITING);

        // 방장으로 입장
        Player host = new Player(userId, nickname);
        host.setRole(PlayerRole.HOST);
        host.setState(PlayerState.READY); // 게임 중 상태 (아직 게임 시작 전이므로 의미없음)
        host.setReadyState(ReadyState.READY); // 방장의 대기방 준비 상태는 READY로 시작

        room.getPlayers().put(userId, host);
        room.getPlayerOrder().add(userId);
        room.setHostId(userId);

        // 문제 정보 검증 및 설정 추가
        Problem selectedProblem = validateAndGetProblem(problemInfo);
        room.setSelectedProblem(selectedProblem);

        // 저장
        roomRepository.save(room);
        roomRepository.setUserRoom(userId, roomId);

        return room;
    }

    // 방 입장
    public JoinRoomResult joinRoom(Long roomId, Long userId, String nickname) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        synchronized (room) {
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
            player.setState(PlayerState.READY); // 게임 중 상태 (아직 게임 시작 전이므로 의미없음)
            player.setReadyState(ReadyState.WAITING); // 참가자의 대기방 준비 상태는 WAITING으로 시작

            room.getPlayers().put(userId, player);
            room.getPlayerOrder().add(userId);

            roomRepository.save(room);
            roomRepository.setUserRoom(userId, roomId);

            // 결과 반환
            boolean isHost = userId.equals(room.getHostId());
            return JoinRoomResult.success(room, player, isHost);
        }
    }

    // 방 퇴장
    public LeaveRoomResult leaveRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId);
        if (room == null || !room.hasPlayer(userId)) {
            // 이미 없으면 삭제된 것으로 처리
            return LeaveRoomResult.roomDeleted(userId);
        }

        // 퇴장 전 상태 확인
        boolean wasHost = userId.equals(room.getHostId());

        // 플레이어 제거
        room.getPlayers().remove(userId);
        room.getPlayerOrder().remove(userId);
        roomRepository.removeUserRoom(userId);

        // 방이 비었으면 삭제
        if (room.isEmpty()) {
            room.setHostId(null);
            roomRepository.delete(roomId);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_DELETED", roomId);
            return LeaveRoomResult.roomDeleted(userId);
        }

        // 방장이 나갔으면 방장 이양
        if (wasHost) {
            // 첫 번째 남은 플레이어를 방장으로
            Long newHostId = room.getPlayerOrder().get(0);
            Player newHost = room.getPlayer(newHostId);
            newHost.setRole(PlayerRole.HOST);
            room.setHostId(newHostId);

            roomRepository.save(room);
            return LeaveRoomResult.hostChanged(room, newHost, userId);
        } else {
            // 일반 참가자가 나간 경우
            roomRepository.save(room);
            return LeaveRoomResult.participantLeft(room, userId);
        }
    }

    // 방 목록 조회
    public RoomListResponse getRooms(RoomListRequest roomListRequest) {
        String state = roomListRequest.getState();

        List<Room> allRooms;

        if (state != null && !state.trim().isEmpty()) {
            // 상태 필터링이 있는 경우
            try {
                RoomState roomState = RoomState.valueOf(state.toUpperCase());
                allRooms = roomRepository.findByState(roomState);
            } catch (IllegalArgumentException e) {
                // 잘못된 상태값인 경우 빈 목록 반환
                allRooms = Collections.emptyList();
            }
        } else {
            allRooms = roomRepository.findAllSorted();
        }

        return RoomListResponse.of(allRooms, state);
    }

    // 방장 권한 넘기기 요청
    public TransferHostRequestResult requestHostTransfer(Long roomId, Long requesterId, Long targetUserId) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        // 요청자가 방장인지 확인
        if (!requesterId.equals(room.getHostId())) {
            throw new RuntimeException("방장만 권한을 넘길 수 있습니다.");
        }

        // 대상자가 방에 있는지 확인
        if (!room.hasPlayer(targetUserId)) {
            throw new RuntimeException("해당 사용자는 방에 없습니다.");
        }

        // 이미 진행 중인 권한 넘기기 요청이 있는지 확인
        if (room.hasPendingHostTransfer()) {
            throw new RuntimeException("이미 진행 중인 권한 넘기기 요청이 있습니다.");
        }

        // 권한 넘기기 요청 상태 설정
        room.setPendingHostTransfer(targetUserId);
        roomRepository.save(room);

        // 요청자와 대상자 정보 조회
        Player requester = room.getPlayer(requesterId);
        Player target = room.getPlayer(targetUserId);

        return TransferHostRequestResult.success(room, requester, target);
    }

    // 방장 권한 넘기기 응답
    public TransferHostResponseResult respondToHostTransfer(Long roomId, Long responderId, boolean accept) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        // 진행 중인 권한 넘기기 요청이 있는지 확인
        if (!room.hasPendingHostTransfer()) {
            throw new RuntimeException("진행 중인 권한 넘기기 요청이 없습니다.");
        }

        // 응답자가 대상자인지 확인
        if (!room.isValidHostTransferTarget(responderId)) {
            throw new RuntimeException("권한 넘기기 요청 대상자가 아닙니다.");
        }

        Long oldHostId = room.getHostId();
        Player oldHost = room.getPlayer(oldHostId);
        Player newHost = room.getPlayer(responderId);

        if (accept) {
            // 수락한 경우: 권한 변경
            // 기존 방장의 역할을 참가자로 변경
            oldHost.setRole(PlayerRole.PARTICIPANT);

            // 새 방장의 역할을 방장으로 변경
            newHost.setRole(PlayerRole.HOST);

            // 방의 hostId 변경
            room.setHostId(responderId);

            // 권한 넘기기 요청 상태 초기화
            room.clearPendingHostTransfer();

            roomRepository.save(room);

            return TransferHostResponseResult.accepted(room, oldHost, newHost);
        } else {
            // 거절한 경우: 상태만 초기화
            room.clearPendingHostTransfer();
            roomRepository.save(room);

            return TransferHostResponseResult.declined(room, oldHost, newHost);
        }
    }

    // 방 설정 변경
    public RoomSettingsUpdateResult updateRoomSettings(Long roomId, Long userId, String nickname, Integer maxPlayers, Integer timeLimit) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        synchronized (room) {
            // 방장 권한 확인
            if (!room.getHostId().equals(userId)) {
                throw new RuntimeException("방장만 방 설정을 변경할 수 있습니다.");
            }

            // 게임 시작 여부 확인
            if (!room.canUpdateSettings()) {
                throw new RuntimeException("게임이 시작된 방의 설정은 변경할 수 없습니다.");
            }

            // 설정 변경 적용
            Integer updatedMaxPlayers = null;
            Integer updatedTimeLimit = null;

            if (maxPlayers != null) {
                room.updateMaxPlayers(maxPlayers);
                updatedMaxPlayers = maxPlayers;
            }

            if (timeLimit != null) {
                room.updateTimeLimit(timeLimit);
                updatedTimeLimit = timeLimit;
            }

            // 변경사항이 없는 경우
            if (updatedMaxPlayers == null && updatedTimeLimit == null) {
                throw new RuntimeException("변경할 설정이 없습니다.");
            }

            // 저장
            roomRepository.save(room);

            log.info("방 설정이 변경되었습니다: roomId={}, hostId={}, maxPlayers={}, timeLimit={}",
                    roomId, userId, updatedMaxPlayers, updatedTimeLimit);

            return RoomSettingsUpdateResult.success(room, updatedMaxPlayers, updatedTimeLimit, userId, nickname);
        }
    }

    // 문제 설정 변경
    public ProblemUpdateResult updateRoomProblem(RoomProblemUpdateRequest request, Long userId, String nickname) {
        // 사용자가 현재 참여 중인 방 ID 조회
        Long currentRoomId = roomRepository.getCurrentRoom(userId);
        if (currentRoomId == null) {
            throw new RuntimeException("현재 참여 중인 방이 없습니다.");
        }

        // 방 조회
        Room room = roomRepository.findById(currentRoomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        synchronized (room) {
            // 권한 확인 - 방장만 문제 변경 가능
            if (!room.getHostId().equals(userId)) {
                throw new RuntimeException("방장만 문제를 변경할 수 있습니다.");
            }

            // 방 상태 확인 - 대기 중일 때만 변경 가능
            if (room.getState() != RoomState.WAITING) {
                throw new RuntimeException("게임이 시작된 방의 문제는 변경할 수 없습니다.");
            }

            try {
                // 문제 정보 생성 및 검증
                RoomCreateRequest.ProblemInfo problemInfo = new RoomCreateRequest.ProblemInfo();
                problemInfo.setProblemId(request.getProblemId());
                problemInfo.setProblemType(request.getProblemType());

                // 문제 유효성 검증 및 조회
                Problem selectedProblem = validateAndGetProblem(problemInfo);

                // 방에 문제 설정
                room.setSelectedProblem(selectedProblem);

                // 저장
                roomRepository.save(room);

                log.info("방 문제 변경됨: roomId={}, hostId={}, problemId={}, problemType={}",
                        room.getRoomId(), userId, request.getProblemId(), request.getProblemType());

                return ProblemUpdateResult.success(room, selectedProblem, userId, nickname);

            } catch (Exception e) {
                log.error("문제 변경 중 오류: roomId={}, userId={}, problemId={}, error={}",
                        room.getRoomId(), userId, request.getProblemId(), e.getMessage());
                throw new RuntimeException("문제 변경 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    // 준비 상태 변경
    public ReadyStateChangeResult changeReadyState(Long roomId, Long userId, ReadyState readyStateStr) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        synchronized (room) {
            // 사용자가 방에 있는지 확인
            if (!room.hasPlayer(userId)) {
                throw new RuntimeException("방에 참가하지 않은 사용자입니다.");
            }

            // 게임이 시작되지 않은 상태인지 확인 (대기 상태에서만 준비 상태 변경 가능)
            if (room.getState() != RoomState.WAITING) {
                throw new RuntimeException("게임이 이미 시작되어 준비 상태를 변경할 수 없습니다.");
            }

            // 준비 상태 변환
            ReadyState newReadyState;
            try {
                newReadyState = readyStateStr;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("유효하지 않은 준비 상태입니다: " + readyStateStr);
            }

            // 플레이어 준비 상태 변경
            Player player = room.getPlayer(userId);
            player.setReadyState(newReadyState);

            // 모든 참가자가 준비 완료되었는지 확인
            boolean allReady = room.getPlayers().values().stream()
                    .allMatch(Player::isReady);

            // 게임 시작 가능 여부 (최소 2명 + 모든 참가자 준비 완료 + 문제 선택됨)
            boolean canStartGame = allReady &&
                    room.getCurrentPlayerCount() >= 2 &&
                    room.getSelectedProblem() != null;

            // 저장
            roomRepository.save(room);

            log.info("준비 상태 변경: roomId={}, userId={}, newReadyState={}, allReady={}, canStartGame={}",
                    roomId, userId, newReadyState, allReady, canStartGame);

            return ReadyStateChangeResult.success(room, userId, newReadyState, allReady, canStartGame);
        }
    }

    private Problem validateAndGetProblem(RoomCreateRequest.ProblemInfo problemInfo) {
        if ("CUSTOM".equals(problemInfo.getProblemType())) {
            // 메모리에서 문제 조회
            return memoryProblemService.findById(problemInfo.getProblemId());
        } else if ("ORIGINAL".equals(problemInfo.getProblemType())) {
            try {
                Long problemId = Long.valueOf(problemInfo.getProblemId());

                // 기존 searchProblems 활용!
                ProblemSearchRequestDto searchDto = new ProblemSearchRequestDto();
                searchDto.setProblemId(problemId);

                Slice<ProblemSummaryDto> result = problemRepositoryCustom.searchProblems(searchDto);

                if (result.isEmpty()) {
                    throw new RuntimeException("문제를 찾을 수 없습니다.");
                }

                return convertToMemoryProblem(result.getContent().get(0));
            } catch (NumberFormatException e) {
                throw new RuntimeException("올바르지 않은 문제 ID 형식입니다.");
            }
        } else {
            throw new RuntimeException("올바르지 않은 문제 타입입니다.");
        }
    }

    private Problem convertToMemoryProblem(ProblemSummaryDto dto) {
        return Problem.builder()
                .problemId(dto.getProblemId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .answer(dto.getAnswer())
                .genre(dto.getGenres())
                .difficulty(com.ssafy.backend.memory.type.Difficulty.valueOf(dto.getDifficulty()))
                .creatorId(Long.valueOf(dto.getCreator().getId()))
                .nickname(dto.getCreator().getNickname())
                .source(com.ssafy.backend.common.enums.Source.valueOf(dto.getSource().toUpperCase()))
                .build();
    }
}
