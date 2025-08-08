package com.ssafy.backend.game.service;

import com.ssafy.backend.entity.User;
import com.ssafy.backend.game.dto.*;
import com.ssafy.backend.memory.*;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.memory.type.AnswerStatus;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.RoomState;
import com.ssafy.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
//    private final ProblemService problemService;

    /**
     * 게임 시작
     */
    @Transactional
    public GameInfoResultDto startGame(Long roomId, Long userId) {
        // todo 삭제 예정 -> 임시 방생성
        Long testRoomNum = roomRepository.getNextRoomId();
        Room testRoom = new Room(testRoomNum, 6, 15);
        testRoom.setState(RoomState.WAITING);
        testRoom.setHostId(1L);
        Player p1 = new Player(1L, "1번");
        p1.setState(PlayerState.READY);
        Player p2 = new Player(2L, "2번");
        p2.setState(PlayerState.READY);
        Player p3 = new Player(3L, "3번");
        p3.setState(PlayerState.READY);
        testRoom.getPlayers().put(1L, p1);
        testRoom.getPlayers().put(2L, p2);
        testRoom.getPlayers().put(3L, p3);
        testRoom.getPlayerOrder().add(1L);
        testRoom.getPlayerOrder().add(2L);
        testRoom.getPlayerOrder().add(3L);
        testRoom.setSelectedProblem(Problem.builder().title("문제 테스트").build());
        roomRepository.save(testRoom);

        log.info("방 번호 test: {}", testRoomNum);

        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        // 유효성 검사
        ValidationResultDto validation = validateGameStart(room, userId);
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getErrorMessage());
        }

        // 방 상태 변경
        synchronized (room) {
            if (room.getState() != RoomState.WAITING) {
                RoomState currentState = room.getState();
                if (currentState == RoomState.STARTING) {
                    throw new RuntimeException("게임 시작 처리 중입니다.");
                } else if (currentState == RoomState.PLAYING) {
                    throw new RuntimeException("이미 게임이 진행 중입니다.");
                } else {
                    throw new RuntimeException("게임을 시작할 수 없는 상태입니다.");
                }
            }
            room.setState(RoomState.STARTING);
        }

        try {
            // 게임 객체 생성 및 초기화
            Game game = new Game(room.getPlayerOrder(), room.getPlayers(), userId);
            room.setCurrentGame(game); // todo; 방장은 어디감요? -> 같이 관리?

            // 최종 상태로 변경 (STARTING -> PLAYING)
            room.setState(RoomState.PLAYING);

            // 저장 (인메모리에서는 이미 반영됨)
            roomRepository.save(room);

            // 게임 정보 반환 (웹소켓 전송용)
            return GameInfoResultDto.builder()
                    .TimeLimit(room.getTimeLimit())
                    .gameInfoResponseDto(createGameInfoDto(room, game))
                    .build();
        } catch (Exception e) {
            // 실패 시 상태 롤백
            room.setState(RoomState.WAITING);
            room.setCurrentGame(null);

            throw new RuntimeException("게임 시작 중 오류가 발생했습니다.");
        }
    }

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

    private ValidationResultDto validateGameStart(Room room, Long userId) {
        // 방장 권한 확인
        if (!userId.equals(room.getHostId())) {
            return ValidationResultDto.invalid("방장만 게임을 시작할 수 있습니다.");
        }

        // 문제 선택 여부 확인
        if (room.getSelectedProblem() == null) {
            return ValidationResultDto.invalid("게임에 사용할 문제가 선택되지 않았습니다.");
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
                .data(GameInfoResponseDto.GameStartedData.builder()
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
                                        .turnIndex(game.getCurrentTurnIndex()).build()) // todo; host 정보 추가
                        .players(
                                new ArrayList<>(game.getPlayers().values())
                        )
                        .build()
                ).build();
    }

    public QuestionResponseDto sendQuestion(Long roomId, QuestionRequestDto questionRequestDto, Long userId) {
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
            // 다음 턴으로 이동 + 질문 횟수 차감
//            game.advanceTurn();

            // 3. 응답 생성
            return QuestionResponseDto.builder()
                    .hostId(room.getHostId())
                    .questionRequestDto(
                            QuestionResponseDto.QuestionRequestDto.builder()
                                    .question(questionRequestDto.getQuestion())
                                    .senderId(userId).build()
                    ).build();

        } catch (Exception e) {
            throw new RuntimeException("질문 처리 중 오류가 발생했습니다.");
        }
    }

    public AnswerResultDto respondToQuestion(Long roomId, AnswerRequestDto answerRequestDto, Long userId) {
        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

        // 유효성 검사
        if (!room.getHostId().equals(userId)) {
            throw new RuntimeException("출제자만 답변할 수 있습니다.");
        }

        if (answerRequestDto.getQuestionerId() == null || answerRequestDto.getQuestion() == null || answerRequestDto.getAnswerStatus() == null) {
            throw new RuntimeException("질문이 유효하지 않습니다.");
        }

        // todo; 중복 유효성 검사

        // 답변 처리 로직
        // QnA 질문-답변 저장
        QnA qna = new QnA(HistoryType.QUESTION, answerRequestDto.getQuestionerId(), answerRequestDto.getQuestion(), answerRequestDto.getAnswerStatus());
        game.addQnA(qna);

        // 질문 횟수 차감
        game.minusRemainingQuestions();

        // 정답 시도 큐 확인
        Optional<AnswerAttempt> nextGuessDto = game.peekOptionalAnswer();

        // 남은 정답 시도 없으면 -> 다음 차례 요청
        if (nextGuessDto.isEmpty()) {
            // 다음 차례 계산하기
            game.advanceTurn();

            Long nextQuestionerId = game.getCurrentQuestionerId();
            User nextQuestioner = userRepository.findByUserId(nextQuestionerId)
                    .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

            return AnswerResultDto.builder()
                    .hasRemainGuess(false)
                    .qnA(qna)
                    .nextTurnDto(NextTurnDto.builder()
                            .nextPlayerId(nextQuestionerId)
                            .nextPlayerNickname(nextQuestioner.getNickname()).build())
                    .nextGuessDto(AnswerResultDto.GuessDto.builder().senderId(null).guess(null).build())
                    .build();
        }

        // 응답 생성
        return AnswerResultDto.builder()
                .hasRemainGuess(true)
                .qnA(qna)
                .nextGuessDto(AnswerResultDto.GuessDto.builder()
                        .senderId(nextGuessDto.get().getUserId())
                        .guess(nextGuessDto.get().getGuess())
                        .build())
                .build();
    }

    public ChatResponseDto sendGuess(Long roomId, QuestionRequestDto guessRequestDto, Long userId) {
        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

//        // todo; util -> 유효성: 게임이 끝났는지 확인
//        if (game.isFinished()) {
//            throw new RuntimeException("게임이 종료되었습니다.");
//        }

        if (guessRequestDto.getQuestion() == null) {
            throw new RuntimeException("정답 시도가 유효하지 않습니다.");
        }
//
//        // 유효성: 플레이어 존재 확인
        Player player = game.getPlayers().get(userId);
//        if (player == null) {
//            throw new RuntimeException("해당 유저는 게임에 참여하고 있지 않습니다.");
//        }

        //아니면 정답 시도 횟수 차감
        player.decrementAnswerAttempt();

        // 정답 시도 큐에 정답 시도 추가
        game.addAnswerAttempt(new AnswerAttempt(userId, guessRequestDto.getQuestion()));

        // 유저 닉네임 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        // 응답 생성
        return ChatResponseDto.builder()
                .senderId(userId)
                .nickname(user.getNickname())
                .message(guessRequestDto.getQuestion())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ChatResponseDto sendChat(Long roomId, ChatRequestDto chatRequestDto, Long userId) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        return ChatResponseDto.builder()
                .senderId(userId)
                .nickname(user.getNickname())
                .message(chatRequestDto.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public JudgeResultDto respondToGuess(Long roomId, JudgeRequestDto judgeRequestDto, Long userId) {
        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

        // 유효성 검사
        if (!room.getHostId().equals(userId)) {
            throw new RuntimeException("출제자만 답변할 수 있습니다.");
        }

        if (judgeRequestDto.getSenderId() == null || judgeRequestDto.getGuess() == null || judgeRequestDto.getAnswerStatus() == null) {
            throw new RuntimeException("정답 판정 값이 유효하지 않습니다.");
        }

        AnswerAttempt answerAttempt = game.peekOptionalAnswer()
                .orElseThrow(() -> new RuntimeException("잘못된 정답 시도 입니다."));
        if (!answerAttempt.getGuess().equals(judgeRequestDto.getGuess()) || !answerAttempt.getUserId().equals(judgeRequestDto.getSenderId())) {
            throw new RuntimeException("잘못된 정답 시도 입니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        // 답변 처리 로직
        try {
            // QnA 정답 시도 - 채점 결과 저장
            game.popAnswer();
            QnA qna = new QnA(HistoryType.GUESS, judgeRequestDto.getSenderId(), judgeRequestDto.getGuess(), judgeRequestDto.getAnswerStatus());
            game.addQnA(qna);

            // 전체 정답 시도 차감
            game.decrementRemainingGuess();

            // 맞았거나
            // 모든 사람의 정답 시도 횟수를 다 소진한 경우
            // 게임 종료
            if (judgeRequestDto.getAnswerStatus() == AnswerStatus.CORRECT || game.getRemainingGuess() <= 0) {
                Problem problem = room.getSelectedProblem();

                EndResponseDto endResponseDto = createEndResponseDto(problem, game.getRemainingQuestions(), judgeRequestDto.getAnswerStatus() == AnswerStatus.CORRECT ? "CORRECT_ANSWER" : "EXHAUSTED_ATTEMPTS",
                        judgeRequestDto.getSenderId(), user.getNickname(), judgeRequestDto.getGuess());

                return JudgeResultDto.builder()
                        .isEnd(true)
                        .qnA(qna)
                        .endResponseDto(endResponseDto)
                        .build();
            }

            // 정답 시도 큐 확인
            Optional<AnswerAttempt> nextGuessDto = game.peekOptionalAnswer();

            // 남은 정답 시도가 없으면 -> 다음 차례 요청
            if (nextGuessDto.isEmpty()) {
                // 다음 차례 계산하기
                game.advanceTurn();

                Long nextQuestionerId = game.getCurrentQuestionerId();
                User nextQuestioner = userRepository.findByUserId(nextQuestionerId)
                        .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

                return JudgeResultDto.builder()
                        .isEnd(false)
                        .hasRemainGuess(false)
                        .qnA(qna)
                        .nextTurnDto(NextTurnDto.builder()
                                .nextPlayerId(nextQuestionerId)
                                .nextPlayerNickname(nextQuestioner.getNickname()).build())
                        .build();
            }

            //남은 정답 시도가 있으면 가장 오래된 정답 시도 보내기
            return JudgeResultDto.builder()
                    .isEnd(false)
                    .qnA(qna)
                    .hasRemainGuess(true)
                    .guessDto(AnswerResultDto.GuessDto.builder()
                            .senderId(nextGuessDto.get().getUserId())
                            .guess(nextGuessDto.get().getGuess()).build())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("정답 판정(채점) 중 오류가 발생했습니다.");
        }

    }

    public NextTurnDto passTurn(Long roomId, Long userId) {
        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

        // todo -> 유저 유효성 검사
        if (!game.getCurrentQuestionerId().equals(userId)) {
            throw new RuntimeException("자신의 차례가 아닙니다.");
        }

        //   todo -> 함수로 빼기
        //    다음 차례 계산하기
        game.advanceTurn();

        Long nextQuestionerId = game.getCurrentQuestionerId();
        User nextQuestioner = userRepository.findByUserId(nextQuestionerId)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        return NextTurnDto.builder()
                .nextPlayerId(nextQuestionerId)
                .nextPlayerNickname(nextQuestioner.getNickname()).build();
    }

    public EndResponseDto endGame(Long roomId) {
        // 방 조회
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new RuntimeException("방을 찾을 수 없습니다.");
        }

        Game game = room.getCurrentGame();
        if (game == null) {
            throw new RuntimeException("게임을 찾을 수 없습니다.");
        }

        Problem problem = room.getSelectedProblem();
        return createEndResponseDto(problem, game.getRemainingQuestions(), "TIMEOUT", null, null, null);
    }

    private EndResponseDto createEndResponseDto(Problem problem, Integer remainingQuestions, String endReason, Long senderId, String senderNickname, String guess) {
        return EndResponseDto.builder()
                .endReason(endReason)
                .winnerInfo(EndResponseDto.WinnerInfoDto.builder()
                        .winnerId(senderId)
                        .nickname(senderNickname).build())
                .problem(EndResponseDto.ProblemDto.builder()
                        .title(problem.getTitle())
                        .content(problem.getContent())
                        .guess(guess)
                        .answer(problem.getAnswer()).build())
                .totalQuestionCount(30 - remainingQuestions)
                .build();
    }
}
