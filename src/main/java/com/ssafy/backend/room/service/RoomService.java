package com.ssafy.backend.room.service;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.RoomState;
import com.ssafy.backend.problem.dto.Request.ProblemSearchRequestDto;
import com.ssafy.backend.problem.dto.Response.ProblemSummaryDto;
import com.ssafy.backend.problem.service.MemoryProblemService;
import com.ssafy.backend.repository.ProblemRepositoryCustom;
import com.ssafy.backend.room.dto.request.RoomCreateRequest;
import com.ssafy.backend.room.dto.request.RoomListRequest;
import com.ssafy.backend.room.dto.response.RoomListResponse;
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
        //room.setTitle(title);
        room.setState(RoomState.WAITING);

        // 방장으로 입장
        Player host = new Player(userId, nickname);
        host.setRole(PlayerRole.HOST);
        host.setState(PlayerState.READY);

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
    public Room joinRoom(Long roomId, Long userId, String nickname) {
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
            player.setState(PlayerState.READY);

            room.getPlayers().put(userId, player);
            room.getPlayerOrder().add(userId);

            roomRepository.save(room);
            roomRepository.setUserRoom(userId, roomId);
        }

        return room;
    }

    // 방 퇴장
    public Room leaveRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId);
        if (room == null || !room.hasPlayer(userId)) {
            return null; // 이미 없으면 성공으로 처리
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

        if (room.isEmpty()) {
            roomRepository.delete(roomId);
            webSocketNotificationService.sendToTopic("/topic/lobby", "ROOM_DELETED", roomId);
            return null; // 방이 삭제됨
        } else {
            roomRepository.save(room);
            return room; // 업데이트된 방 정보 반환
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
