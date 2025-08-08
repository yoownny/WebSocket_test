package com.ssafy.backend.game.controller;

import com.ssafy.backend.game.dto.*;
import com.ssafy.backend.game.service.GameService;
import com.ssafy.backend.game.service.GameTimerService;
import com.ssafy.backend.websocket.service.WebSocketNotificationService;
import com.ssafy.backend.websocket.util.WebSocketUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GameController", description = "게임 진행 관련 기능")
public class GameController {

    private final GameService gameService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final GameTimerService gameTimerService;
//    private final RoomService roomService;
//    private final LobbyService lobbyService;

    /**
     * 게임 시작
     */
    @Operation(description = "게임 시작")
    @MessageMapping("/games/{roomId}/start")
    public void startGame(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        log.info("게임 시작 요청");
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            GameInfoResultDto result = gameService.startGame(roomId, userId);
            log.info("게임 시작 성공: roomId={}", roomId);

            // 게임 시작 타이머 설정
            gameTimerService.startGameTimer(roomId, result.getTimeLimit(), () -> {
                EndResponseDto endResponseDto = gameService.endGame(roomId);
                endResponseDto.setPlayTime(gameTimerService.getElapsedTimeFormatted(roomId));
                webSocketNotificationService.sendToTopic("/topic/games/" + roomId, "END_GAME", endResponseDto);
            });


            // 방에 있는 모든 사용자에게 게임 시작 알림
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/game-started", "GAME_STARTED", result.getGameInfoResponseDto());
            // todo; 로비에 방 상태 변경 알림 (게임 중으로 표시)
            /*                List<RoomInfoDto> updatedRooms = lobbyService.getAllRooms();
                messagingTemplate.convertAndSend(
                        "/sub/lobby/rooms-updated",
                        updatedRooms
                );*/
        } catch (
                Exception e) {
            log.error("게임 시작 처리 중 예외: userId={}, roomId={}, error={}",
                    userId, roomId, e.getMessage());

            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

    /**
     * 질문 제출
     */
    @Operation(description = "질문 제출")
    @MessageMapping("/games/{roomId}/question")
    public void sendQuestion(@DestinationVariable Long roomId, @Payload QuestionRequestDto questionRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("질문 제출: {}", roomId);
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            QuestionResponseDto result = gameService.sendQuestion(roomId, questionRequestDto, userId);
            log.info("질문 제출 성공 hostId={}", result.getHostId());

            // 출제자에게 "질문이 잘 도착함" 알림
            webSocketNotificationService.sendToUser(result.getHostId(), "/queue/game", "QUESTION_SEND", result);

            // 질문 정보를 모든 사용자 채팅에 broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/chat", "QUESTION", result);
        } catch (
                Exception e) {
            log.warn("질문 제출 실패: userId={}, roomId={}, reason={}", userId, roomId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

    /**
     * 답변 제출
     */
    @Operation(description = "답변 제출")
    @MessageMapping("/games/{roomId}/respond-question")
    public void respondToQuestion(@DestinationVariable Long roomId, @Payload AnswerRequestDto answerRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("답변 제출: {}", roomId);
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            AnswerResultDto result = gameService.respondToQuestion(roomId, answerRequestDto, userId);
            log.info("답변 제출 성공");

            // 답변 정보를 모든 사용자 채팅에 broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/chat", "RESPOND_QUESTION",
                    AnswerResponseDto.builder().qnA(result.getQnA()).nextGuessDto(result.getNextGuessDto()).build());

            // 질문 - 답변 QnAHistory broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/history", "QUESTION",
                    result.getQnA());

            // 출제자에게 "답변이 잘 갔음" 알림
            webSocketNotificationService.sendToUser(
                    userId, // 출제자 본인
                    "/queue/game",
                    "RESPOND_QUESTION",
                    AnswerResponseDto.builder().qnA(result.getQnA()).nextGuessDto(result.getNextGuessDto()).build());

            // todo; turn 넘기기 동작 -> players 전체 리스트와 현재 턴을 보내야 하는지
            // 남은 정답 시도가 없으면 turn pass
            if (!result.getHasRemainGuess()) {
                // 현재 유저에게 다음 턴 알림
                webSocketNotificationService.sendToUser(userId, "/queue/game", "NEXT_TURN",
                        result.getNextTurnDto()); // 남은 정답 시도
                // 다음 턴 유저에게 다음 턴 알림
                webSocketNotificationService.sendToUser(result.getNextTurnDto().getNextPlayerId(), "/queue/game", "NEXT_TURN",
                        result.getNextTurnDto()); // 남은 정답 시도
            }

            log.info("답변에 대하여 질문={}, 답={}, 정답시도 리스트={}", result.getQnA().getQuestion(), result.getQnA().getAnswer(), result.getNextGuessDto().getGuess());
        } catch (Exception e) {
            log.warn("답변 제출 실패: userId={}, roomId={}, reason={}", userId, roomId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

    /**
     * 정답 시도 (추리하기)
     */
    @Operation(description = "정답 시도 (추리하기)")
    @MessageMapping("/games/{roomId}/guess")
    public void sendGuess(@DestinationVariable Long roomId, @Payload QuestionRequestDto guessRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("정답 시도: {}", roomId);
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            ChatResponseDto result = gameService.sendGuess(roomId, guessRequestDto, userId);
            log.info("정답 시도 제출 성공");

            // 정답 시도자에게 알림
            webSocketNotificationService.sendToUser(userId, "/queue/game", "GUESS_SEND", result);

            // 정답 시도를 모든 사용자 채팅에 broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/chat", "RESPOND_GUESS", result);

            log.info("정답 시도 senderId={}, message={}", result.getSenderId(), result.getMessage());
        } catch (Exception e) {
            log.warn("정답 시도 실패: userId={}, roomId={}, reason={}", userId, roomId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

    /**
     * 정답 판정(채점)
     */
    @Operation(description = "정답 판정(채점)")
    @MessageMapping("/games/{roomId}/respond-guess")
    public void respondToGuess(@DestinationVariable Long roomId, @Payload JudgeRequestDto judgeRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("정답 판정(채점): {}", roomId);
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            JudgeResultDto result = gameService.respondToGuess(roomId, judgeRequestDto, userId);
            log.info("정답 판정(채점) 성공");

            // 정답 판정(채점) 결과를 모든 사용자 채팅에 broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/chat", "RESPOND_GUESS",
                    result.getQnA()); // 채팅 - QnA

            if (result.getIsEnd()) {
                // playTime 설정
                result.getEndResponseDto().setPlayTime(gameTimerService.getElapsedTimeFormatted(roomId));
                // 타이머 삭제
                gameTimerService.cancelGameTimer(roomId);

                // 게임 종료를 모든 사용자에게 broadcast
                webSocketNotificationService.sendToTopic("/topic/games/" + roomId, "END_GAME",
                        result.getEndResponseDto()); // 게임 종료 response
                log.debug("게임 종료: reason={}", result.getEndResponseDto().getEndReason());
            } else {
                // 정답 시도 - 채점 결과 QnAHistory broadcast
                webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/history", "GUESS",
                        result.getQnA()); // QnA
                if (result.getHasRemainGuess()) {
                    // 출제자에게 남은 정답 시도 알림
                    webSocketNotificationService.sendToUser(userId, "/queue/game", "GUESS_SEND",
                            result.getGuessDto()); // 남은 정답 시도
                    log.debug("남은 정답 시도: guess={}", result.getGuessDto());
                } else {
                    // 현재 유저에게 다음 턴 알림
                    webSocketNotificationService.sendToUser(userId, "/queue/game", "NEXT_TURN",
                            result.getNextTurnDto()); // 남은 정답 시도
                    // 다음 턴 유저에게 다음 턴 알림
                    webSocketNotificationService.sendToUser(result.getNextTurnDto().getNextPlayerId(), "/queue/game", "NEXT_TURN",
                            result.getNextTurnDto()); // 남은 정답 시도
                    log.debug("다음 차례: nextId={}", result.getNextTurnDto().getNextPlayerId());
                }
            }
        } catch (Exception e) {
            log.warn("정답 판정(채점) 실패: userId={}, roomId={}, reason={}", userId, roomId, e.getMessage());
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }


    /**
     * 채팅
     */
    @Operation(description = "채팅")
    @MessageMapping("/games/{roomId}/chat")
    public void sendChat(@DestinationVariable Long roomId, @Payload ChatRequestDto chatRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            ChatResponseDto result = gameService.sendChat(roomId, chatRequestDto, userId);
            // 모든 사용자 채팅에 broadcast
            webSocketNotificationService.sendToTopic("/topic/games/" + roomId + "/chat", "CHAT", result);
            log.info("chat: {}, {}, {}, {}", result.getSenderId(), result.getNickname(), result.getMessage(), result.getTimestamp());
        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

    /**
     * 턴 패스 혹은 시간 초과
     */
    @Operation(description = "턴 패스 혹은 시간 초과")
    @MessageMapping("/games/{roomId}/pass-turn")
    public void passTurn(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = null;
        try {
            userId = WebSocketUtils.getUserIdFromSession(headerAccessor);
            NextTurnDto result = gameService.passTurn(roomId, userId);
            // 현재 유저에게 다음 턴 알림
            webSocketNotificationService.sendToUser(userId, "/queue/game", "NEXT_TURN",
                    result); // 남은 정답 시도
            // 다음 턴 유저에게 다음 턴 알림
            webSocketNotificationService.sendToUser(result.getNextPlayerId(), "/queue/game", "NEXT_TURN",
                    result); // 남은 정답 시도
            log.debug("다음 차례: nextId={}", result.getNextPlayerId());
        } catch (Exception e) {
            webSocketNotificationService.sendToUser(userId, "/queue/game", "ERROR", e.getMessage());
        }
    }

//    /**
//     * 방 나가기 처리
//     */
//    @Operation(description = "방 나가기", method = "MESSAGE")
//    @MessageMapping("/room/leave")
//    public void leaveRoom(RoomLeaveRequestDto requestDto) {
//        log.debug("방 나가기 요청: {}", requestDto);
//
//        try {
//            // 1. 서비스에서 방 나가기 처리
//            RoomLeaveResult result = roomService.leaveRoom(requestDto);
//
//            if (result.isSuccess()) {
//                // 2-1. 방에 남은 사용자들에게 퇴장 알림
//                messagingTemplate.convertAndSend(
//                        "/sub/room/" + requestDto.getRoomId() + "/player-left",
//                        new PlayerLeftResponseDto(requestDto.getUserId())
//                );
//
//                // 2-2. 로비 방 목록 업데이트
//                List<RoomInfoDto> updatedRooms = lobbyService.getAllRooms();
//                messagingTemplate.convertAndSend(
//                        "/sub/lobby/rooms-updated",
//                        updatedRooms
//                );
//            }
//
//        } catch (Exception e) {
//            log.error("방 나가기 처리 중 오류: {}", e.getMessage());
//        }
//    }
//

//
//    /**
//     * 연결 해제 시 자동 호출
//     * 연결 상태 추적 및 정리 작업
//     */
//    @EventListener
//    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
//        try {
//            // 세션에서 사용자 정보 추출 (연결 시 저장했다고 가정)
//            String userId = (String) event.getSessionAttributes().get("userId");
//            String currentRoomId = (String) event.getSessionAttributes().get("currentRoomId");
//
//            if (userId != null) {
//                log.info("사용자 연결 해제: userId={}, roomId={}", userId, currentRoomId);
//
//                // 1. 서비스에서 연결 해제 처리
//                DisconnectResult result = roomService.handleDisconnect(userId, currentRoomId);
//
//                if (result.wasInRoom()) {
//                    // 2. 방에 있었다면 다른 사용자들에게 알림
//                    messagingTemplate.convertAndSend(
//                            "/sub/room/" + currentRoomId + "/player-disconnected",
//                            new PlayerDisconnectedResponseDto(userId)
//                    );
//
//                    // 3. 게임 중이었다면 게임 로직 처리
//                    if (result.wasInGame()) {
//                        gameService.handlePlayerDisconnected(currentRoomId, userId);
//
//                        // 게임 상태 변경 알림
//                        messagingTemplate.convertAndSend(
//                                "/sub/room/" + currentRoomId + "/game-state-changed",
//                                result.getGameStateInfo()
//                        );
//                    }
//
//                    // 4. 로비 방 목록 업데이트
//                    List<RoomInfoDto> updatedRooms = lobbyService.getAllRooms();
//                    messagingTemplate.convertAndSend(
//                            "/sub/lobby/rooms-updated",
//                            updatedRooms
//                    );
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("연결 해제 처리 중 오류: {}", e.getMessage());
//        }
//    }
}

