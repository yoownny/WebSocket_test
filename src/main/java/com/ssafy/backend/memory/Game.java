package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.PlayerRole;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

// 한 판 게임 데이터
@Getter
public class Game {
    // 게임 진행 정보
    private Problem currentProblem; // todo; 없앨 수도 있음 -> 이번 게임 문제
    private int remainingQuestions; // 전체 남은 질문 수

    // 플레이어 정보
    private List<Long> turnOrder; // 턴 순서 (랜덤)
    @Setter
    private Long currentQuestionerId; // 현재 질문자
    @Setter
    private int currentTurnIndex = 0; // 현재 턴 인덱스
    private ConcurrentHashMap<Long, Player> players = new ConcurrentHashMap<>(); // 플레이어 상세 정보

    // 게임 데이터 (한 판 끝나면 모두 삭제)
    private final Queue<AnswerAttempt> answerQueue = new ConcurrentLinkedQueue<>(); // 정답 시도 대기열
    private final List<QnA> gameHistory = Collections.synchronizedList(new ArrayList<>()); // 질문-답변 기록 (시간 순 저장)

    // 타이머
    @Setter
    private long gameStartTime; // 게임 시작 시간
//    private long turnStartTime; // 현재 턴 시작 시간

    public Game(List<Long> playerIds, Map<Long, Player> roomPlayers) {
        // 턴 설정
        initGameInitialInfo(playerIds);

        // 플레이어 상태 초기화
        initializePlayersFrom(roomPlayers);

        // 첫 턴 설정
        setFirstTurn();
    }

    private void initGameInitialInfo(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            throw new IllegalArgumentException("플레이어 정보가 없습니다.");
        }
        turnOrder = new ArrayList<>(playerIds);
        Collections.shuffle(turnOrder);
        remainingQuestions = 30;
        gameStartTime = System.currentTimeMillis(); // todo; 이걸 여기서 검증하는게 맞나?
    }

    public void initializePlayersFrom(Map<Long, Player> roomPlayers) {
        players = (ConcurrentHashMap<Long, Player>) roomPlayers.values().stream()
                .map(gamePlayer -> {
                    // 복사 생성자로 게임용 Player 생성
                    // Player gamePlayer = Player.forGame(roomPlayer);
                    // todo; 얕은 복사 상태임
                    // todo; 방장(출제자)는 제외

                    // 역할 설정
                    if (gamePlayer.getUserId().equals(currentQuestionerId)) {
                        gamePlayer.setRole(PlayerRole.QUESTIONER);
                    } else {
                        gamePlayer.setRole(PlayerRole.PARTICIPANT);
                    }

                    return gamePlayer;
                })
                .collect(Collectors.toConcurrentMap(
                        Player::getUserId,
                        Function.identity()
                ));
    }

    // 첫 턴 설정
    private void setFirstTurn() {
        if (!turnOrder.isEmpty()) {
            currentQuestionerId = turnOrder.getFirst();
            currentTurnIndex = 0;
        }
    }

    // 안전한 gameHistory getter - 방어적 복사
    public List<QnA> getGameHistory() {
        synchronized (gameHistory) {
            return new ArrayList<>(gameHistory);
        }
    }

    // 가장 최신 질문 반환 -> 마지막 질문이 무조건 답변 대기중인 질문
    public QnA getLastQnA() {
        synchronized (gameHistory) {
            return gameHistory.isEmpty() ? null : gameHistory.getLast();
        }
    }

    // 안전한 QnA 추가
    public void addQnA(QnA qna) {
        gameHistory.add(qna); // Collections.synchronizedList의 add()는 이미 동기화됨
    }

    public int getGameHistorySize() {
        return gameHistory.size(); // Collections.synchronizedList의 size()는 이미 동기화됨
    }

    public boolean hasGameHistory() {
        return !gameHistory.isEmpty(); // isEmpty()도 이미 동기화됨
    }


    // 턴 관리
    // 주어진 플레이어가 현재 질문자인지 확인
    public boolean isCurrentQuestioner(Long playerId) {
        return Objects.equals(currentQuestionerId, playerId);
    }

    // 현재 턴이 유효한지 확인 (질문자가 존재하고 게임이 진행 중인지)
    public boolean validateTurn() {
        return currentQuestionerId != null && !isFinished();
    }

    // 현재 턴 관리 메서드 추가
    public Long getNextQuestioner() {
        if (turnOrder.isEmpty()) return null;

        int nextIndex = (currentTurnIndex + 1) % turnOrder.size();
        return turnOrder.get(nextIndex);
    }

    // 다음 턴으로 넘기기
    public void advanceTurn() {
        if (turnOrder.isEmpty()) return;

        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
        currentQuestionerId = turnOrder.get(currentTurnIndex);
        remainingQuestions--;
    }

    // 특정 플레이어가 나갔을 때 턴 순서 조정
    public void removePlayerFromTurn(Long playerId) {
        int removedIndex = turnOrder.indexOf(playerId);
        if (removedIndex == -1) return;

        turnOrder.remove(playerId);

        if (turnOrder.isEmpty()) {
            currentQuestionerId = null;
            return;
        }

        // 현재 턴 인덱스 조정
        if (removedIndex <= currentTurnIndex && currentTurnIndex > 0) {
            currentTurnIndex--;
        }

        // 턴 순서 재조정
        currentTurnIndex = currentTurnIndex % turnOrder.size();
        currentQuestionerId = turnOrder.get(currentTurnIndex);
    }

    // 게임 상태 확인
    public boolean isFinished() {
        return remainingQuestions <= 0 || turnOrder.isEmpty();
    }

    // Getters and Setters
//    public void setTurnStartTime(long turnStartTime) { this.turnStartTime = turnStartTime; }

    public void addAnswerAttempt(AnswerAttempt attempt) {
        answerQueue.offer(attempt);
    }

    // 정답 시도 큐
    public Optional<AnswerAttempt> peekOptionalAnswer() {
        return Optional.ofNullable(answerQueue.peek());
    }

    public AnswerAttempt popAnswer() {
        return answerQueue.poll();
    }
}
