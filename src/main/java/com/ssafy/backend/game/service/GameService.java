package com.ssafy.backend.game.service;

import com.ssafy.backend.game.dto.GameInfoResponseDto;
import com.ssafy.backend.game.dto.GameStartResponseDto;
import com.ssafy.backend.game.dto.QuestionResponseDto;
import com.ssafy.backend.game.dto.QuestionResponseDto.QnAData;
import com.ssafy.backend.game.dto.ValidationResultDto;
import com.ssafy.backend.memory.type.AnswerStatus;
import com.ssafy.backend.memory.Game;
import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.Problem;
import com.ssafy.backend.memory.QnA;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.type.RoomState;
import com.ssafy.backend.memory.repository.RoomRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomRepository roomRepository;
//    private final ProblemService problemService;

    /**
     * 게임 시작
     */
    @Transactional
    public GameStartResponseDto startGame(Long roomId, Long userId) {
        // todo 삭제 예정 -> 임시 방생성
        Long testRoomNum = roomRepository.getNextRoomId();
        Room testRoom = new Room(testRoomNum, 6, 15);
        testRoom.setHostId(1L);
        Player p1 = new Player(1L, "1번");
        p1.setState(PlayerState.READY);
        Player p2 = new Player(2L, "2번");
        p2.setState(PlayerState.READY);
        testRoom.getPlayers().put(1L, p1);
        testRoom.getPlayers().put(2L, p2);
        testRoom.getPlayerOrder().add(1L);
        testRoom.getPlayerOrder().add(2L);
        testRoom.setSelectedProblem(Problem.builder().title("문제 테스트").build());
        roomRepository.save(testRoom);

        log.info("방 번호 test: {}", testRoomNum);
        // todo throw로 변경예정;

        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            return GameStartResponseDto.failure("방을 찾을 수 없습니다.");
        }

        // 유효성 검사
        ValidationResultDto validation = validateGameStart(room, userId);
        if (!validation.isValid()) {
            return GameStartResponseDto.failure(validation.getErrorMessage());
        }

        // 방 상태 변경
        synchronized (room) {
            if (room.getState() != RoomState.WAITING) {
                RoomState currentState = room.getState();
                if (currentState == RoomState.STARTING) {
                    return GameStartResponseDto.failure("게임 시작 처리 중입니다.");
                } else if (currentState == RoomState.PLAYING) {
                    return GameStartResponseDto.failure("이미 게임이 진행 중입니다.");
                } else {
                    return GameStartResponseDto.failure("게임을 시작할 수 없는 상태입니다.");
                }
            }
            room.setState(RoomState.STARTING);
        }

        try {
            // 게임 객체 생성 및 초기화
            Game game = new Game(room.getPlayerOrder(), room.getPlayers());
            room.setCurrentGame(game); // todo; 방장은 어디감요? -> 같이 관리?

            // 최종 상태로 변경 (STARTING -> PLAYING)
            room.setState(RoomState.PLAYING);

            // 저장 (인메모리에서는 이미 반영됨)
            roomRepository.save(room);

            log.info("게임 시작 완료: roomId={}, players={}",
                    room.getRoomId(), game.getTurnOrder().size());

            // 게임 정보 반환 (웹소켓 전송용)
            return GameStartResponseDto.success(createGameInfoDto(room, game));

        } catch (Exception e) {
            // 실패 시 상태 롤백
            room.setState(RoomState.WAITING);
            room.setCurrentGame(null);
            log.error("게임 시작 실패, 상태 롤백: roomId={}", room.getRoomId(), e);

            return GameStartResponseDto.failure("게임 시작 중 오류가 발생했습니다.");
        }
    }

//    /**
//     * 답변 제출 처리
//     */
//    public AnswerResult submitAnswer(AnswerSubmitRequestDto requestDto) {
//        log.debug("답변 처리: userId={}, answer={}", requestDto.getUserId(), requestDto.getAnswer());
//
//        Room room = roomRepository.findById(requestDto.getRoomId())
//                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
//
//        Game game = room.getCurrentGame();
//        if (game == null) {
//            throw new IllegalStateException("진행 중인 게임이 없습니다.");
//        }
//
//        // 1. 답변 검증
//        boolean isCorrect = validateAnswer(game.getCurrentProblem(), requestDto.getAnswer());
//
//        // 2. 정답인 경우 다음 턴 진행
//        NextTurnInfo nextTurnInfo = null;
//        if (isCorrect) {
//            nextTurnInfo = proceedToNextTurn(room, game);
//        }
//
//        // 3. 게임 히스토리 저장
//        saveAnswerHistory(game, requestDto.getUserId(), requestDto.getAnswer(), isCorrect);
//
//        return AnswerResult.builder()
//                .correct(isCorrect)
//                .message(isCorrect ? "정답입니다!" : "틀렸습니다.")
//                .nextTurnInfo(nextTurnInfo)
//                .build();
//    }
//
//    /**
//     * 플레이어 연결 해제 처리
//     */
//    public void handlePlayerDisconnected(String roomId, String userId) {
//        log.info("플레이어 연결 해제 처리: roomId={}, userId={}", roomId, userId);
//
//        Room room = roomRepository.findById(roomId).orElse(null);
//        if (room == null || room.getCurrentGame() == null) {
//            return;
//        }
//
//        Game game = room.getCurrentGame();
//
//        // 현재 질문자가 나간 경우 다음 턴으로 진행
//        if (userId.equals(game.getCurrentQuestionerId())) {
//            log.info("현재 질문자 연결 해제, 다음 턴 진행: userId={}", userId);
//            proceedToNextTurn(room, game);
//        }
//
//        // 게임 종료 조건 확인 (남은 플레이어가 1명 이하)
//        long remainingPlayers = room.getPlayers().values().stream()
//                .filter(player -> player.getState() == PlayerState.PLAYING)
//                .count();
//
//        if (remainingPlayers <= 1) {
//            endGame(room, "플레이어 부족으로 게임 종료");
//        }
//    }

    // === Private 메서드들 (순수 비즈니스 로직) ===

    private ValidationResultDto validateGameStart(Room room, Long userId) { // todo; 여기서 바로 throw?
        // 방장 권한 확인
        if (!userId.equals(room.getHostId())) {
            return ValidationResultDto.invalid("방장만 게임을 시작할 수 있습니다.");
        }

        // 문제 선택 여부 확인
        if (room.getSelectedProblem() == null) {
            return ValidationResultDto.invalid("게임에 사용할 문제가 선택되지 않았습니다.");
        }

        // 방 상태 확인
        if (room.getState() != RoomState.WAITING) {
            return ValidationResultDto.invalid("대기 중인 방에서만 게임을 시작할 수 있습니다.");
        }

        // 최소 인원 확인
        if (room.getPlayers().size() < 2) {
            return ValidationResultDto.invalid("최소 2명 이상이어야 게임을 시작할 수 있습니다.");
        }

        // 모든 참가자 준비 완료 확인
        boolean allReady = room.getPlayers().values().stream()
                .allMatch(player -> player.getState() == PlayerState.READY);

        if (!allReady) {
            return ValidationResultDto.invalid("모든 참가자가 준비 완료 상태여야 합니다.");
        }

        return ValidationResultDto.valid();
    }

    private GameInfoResponseDto createGameInfoDto(Room room, Game game) { // todo; 문제 정보도 보내야 하는지?
        return GameInfoResponseDto.builder()
                .data(
                        GameInfoResponseDto.GameStartedData.builder()
                                .roomId(room.getRoomId())
                                .roomState(room.getState())
                                .gameStatus(
                                        GameInfoResponseDto.GameStatus.builder()
                                                .remainingQuestions(room.getCurrentGame().getRemainingQuestions())
                                                .totalQuestions(30).build()) //todo; 직접 넣는게 맞나?
                                .currentTurn(
                                        GameInfoResponseDto.CurrentTurn.builder()
                                                .questionerId(game.getCurrentQuestionerId())
                                                .nickname("????????") //todo; 이걸 넣어야 하는지?
                                                .turnIndex(game.getCurrentTurnIndex()).build())
                                .players(
                                        new ArrayList<>(room.getPlayers().values())
                                ) //todo; 게임에 있어야 하나?
                                .build()
                )
                .build();
    }

    public QuestionResponseDto sendQuestion(Long roomId, String message, Long userId) {
        // 방 조회 //todo;util로 뺴기
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

        // 유효성 검사
        log.info("currentId={}, userId={}", game.getCurrentQuestionerId(), userId);
        if (!game.isCurrentQuestioner(userId)) {
            throw new RuntimeException("질문자의 차례가 아닙니다.");
        }
        if (!game.validateTurn()) {
            throw new RuntimeException("더 이상 질문을 할 수 없습니다.");
        }

        // 질문 처리 로직
        try {
            // 질문 저장
            QnA qna = new QnA(userId, message, AnswerStatus.PENDING);
            game.addQnA(qna);

            // 다음 턴으로 이동 + 질문 횟수 차감
            game.advanceTurn();

            // 3. 응답 생성
            return QuestionResponseDto.builder()
                    .data(
                            QnAData.builder()
                                    .hostId(room.getHostId())
                                    .questionRequestDto(
                                            QuestionResponseDto.QuestionRequestDto.builder()
                                                .message(message)
                                                .senderId(userId).build()
                                    ).build()
                    ).build();

        } catch (Exception e) {
            throw new RuntimeException("질문 처리 중 오류가 발생했습니다.");
        }
    }

//    private boolean validateAnswer(Problem problem, String answer) {
//        // 정답 검증 로직
//        return problem.getCorrectAnswer().equalsIgnoreCase(answer.trim());
//    }
//
//    private NextTurnInfo proceedToNextTurn(Room room, Game game) {
//        // 다음 턴으로 진행하는 로직
//        List<String> turnOrder = game.getTurnOrder();
//        int currentIndex = game.getCurrentTurnIndex();
//        int nextIndex = (currentIndex + 1) % turnOrder.size();
//
//        game.setCurrentTurnIndex(nextIndex);
//        game.setCurrentQuestionerId(turnOrder.get(nextIndex));
//        game.setTurnStartTime(System.currentTimeMillis());
//
//        // 새로운 문제 설정
//        Problem nextProblem = problemService.getRandomProblem();
//        game.setCurrentProblem(nextProblem);
//
//        // 남은 질문 수 감소
//        game.setRemainingQuestions(game.getRemainingQuestions() - 1);
//
//        return NextTurnInfo.builder()
//                .currentQuestionerId(game.getCurrentQuestionerId())
//                .currentProblem(convertProblemToDto(nextProblem))
//                .remainingQuestions(game.getRemainingQuestions())
//                .build();
//    }
//
//    private void endGame(Room room, String reason) {
//        room.setState(RoomState.FINISHED);
//        log.info("게임 종료: roomId={}, reason={}", room.getRoomId(), reason);
//    }
}
