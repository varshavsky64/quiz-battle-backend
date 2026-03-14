package com.github.varshavsky64.quizbattle.service.impl;

import com.github.varshavsky64.quizbattle.config.AppProperties;
import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.domain.response.MatchHistoryResponse;
import com.github.varshavsky64.quizbattle.domain.ws.*;
import com.github.varshavsky64.quizbattle.match.MatchSession;
import com.github.varshavsky64.quizbattle.match.PlayerRoundState;
import com.github.varshavsky64.quizbattle.match.RoundResult;
import com.github.varshavsky64.quizbattle.repository.MatchRepository;
import com.github.varshavsky64.quizbattle.repository.PlayerRepository;
import com.github.varshavsky64.quizbattle.service.MatchPersistenceService;
import com.github.varshavsky64.quizbattle.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final AppProperties appProperties;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchPersistenceService matchPersistenceService;

    private final Map<UUID, MatchSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public MatchEntity getMatch(UUID id) {
        return matchRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchHistoryResponse> getPlayerHistory(UUID playerId, int page, int size) {
        return matchRepository
                .findByPlayerOneIdOrPlayerTwoIdOrderByStartedAtDesc(playerId, playerId, PageRequest.of(page, size))
                .map(m -> new MatchHistoryResponse(m, playerId));
    }

    @Override
    public void startMatch(MatchSession session) {
        activeSessions.put(session.getMatchId(), session);

        sendMatchFound(session, session.getPlayerOneId());
        if (!session.isBot()) {
            sendMatchFound(session, session.getPlayerTwoId());
        }

        sendRoundStart(session, session.getPlayerOneId());
        if (!session.isBot()) {
            sendRoundStart(session, session.getPlayerTwoId());
        }

        scheduleRoundTimer(session, session.getPlayerOneId());
        if (!session.isBot()) {
            scheduleRoundTimer(session, session.getPlayerTwoId());
        }

        if (session.isBot()) {
            scheduleBotFinish(session);
        }
    }

    @Override
    public synchronized void processAnswer(UUID playerId, UUID matchId, String answerId) {
        MatchSession session = activeSessions.get(matchId);
        if (session == null) {
            log.warn("No active session for match {}", matchId);
            return;
        }

        PlayerRoundState state = session.stateFor(playerId);
        if (state == null || state.isFinished()) return;

        int roundIndex = state.getCurrentRound();
        if (roundIndex >= session.getQuestions().size()) return;

        state.cancelTimer();

        QuestionEntity question = session.getQuestions().get(roundIndex);
        String correctAnswerId = question.getAnswers().stream()
                .filter(AnswerEntity::isCorrect)
                .map(AnswerEntity::getId)
                .findFirst().orElse(null);

        boolean correct = answerId != null && answerId.equals(correctAnswerId);
        RoundResult result = new RoundResult(roundIndex + 1, question.getId(), answerId, correctAnswerId, correct, LocalDateTime.now());
        state.addResult(result);

        RoundEndPayload roundEnd = RoundEndPayload.builder()
                .roundNumber(roundIndex + 1)
                .correctAnswerId(correctAnswerId)
                .yourAnswerId(answerId)
                .correct(correct)
                .build();
        send(playerId, "ROUND_END", roundEnd);

        if (state.getCurrentRound() >= session.getQuestions().size()) {
            state.markFinished();
            finishPlayerPart(session, playerId);
        } else {
            taskScheduler.schedule(
                    () -> {
                        sendRoundStart(session, playerId);
                        scheduleRoundTimer(session, playerId);
                    },
                    Instant.now().plusSeconds(2)
            );
        }
    }

    @Override
    public void requestNextRound(UUID playerId, UUID matchId) {
        MatchSession session = activeSessions.get(matchId);
        if (session == null) return;

        PlayerRoundState state = session.stateFor(playerId);
        if (state == null || state.isFinished()) return;

        if (state.getCurrentRound() < session.getQuestions().size()) {
            sendRoundStart(session, playerId);
            scheduleRoundTimer(session, playerId);
        }
    }

    private void finishPlayerPart(MatchSession session, UUID playerId) {
        UUID opponentId = session.opponentId(playerId);
        PlayerRoundState myState = session.stateFor(playerId);

        boolean opponentFinished;
        int opponentScore;
        List<RoundEndPayload> opponentRounds = null;

        if (session.isBot()) {
            opponentFinished = Instant.now().isAfter(session.getBotFinishAt());
            opponentScore = (int) session.getBotResults().stream().filter(RoundResult::isCorrect).count();
            if (opponentFinished) {
                opponentRounds = toRoundEndPayloads(session.getBotResults());
            }
        } else {
            PlayerRoundState oppState = session.stateFor(opponentId);
            opponentFinished = oppState != null && oppState.isFinished();
            opponentScore = opponentFinished ? oppState.score() : 0;
            if (opponentFinished) {
                opponentRounds = toRoundEndPayloads(oppState.getResults());
            }
        }

        String opponentName = getOpponentName(session, playerId);

        MatchEndPayload matchEnd = MatchEndPayload.builder()
                .rounds(toRoundEndPayloads(myState.getResults()))
                .opponentRounds(opponentRounds)
                .yourScore(myState.score())
                .opponentScore(opponentScore)
                .winnerId(opponentFinished ? determineWinner(session, myState.score(), opponentScore, playerId, opponentId) : null)
                .opponentFinished(opponentFinished)
                .opponentName(opponentName)
                .build();
        send(playerId, "MATCH_END", matchEnd);

        if (!session.isBot() && opponentFinished) {
            PlayerRoundState oppState = session.stateFor(opponentId);
            UUID winnerId = determineWinner(session, oppState.score(), myState.score(), opponentId, playerId);
            MatchUpdatedPayload updated = MatchUpdatedPayload.builder()
                    .opponentRounds(toRoundEndPayloads(myState.getResults()))
                    .opponentScore(myState.score())
                    .winnerId(winnerId)
                    .build();
            send(opponentId, "MATCH_UPDATED", updated);
            if (session.claimPersist()) {
                matchPersistenceService.persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
        } else if (session.isBot() && opponentFinished) {
            if (session.claimPersist()) {
                matchPersistenceService.persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
        }
    }

    private void scheduleBotFinish(MatchSession session) {
        taskScheduler.schedule(() -> {
            UUID humanId = session.getPlayerOneId();
            PlayerRoundState humanState = session.stateFor(humanId);
            if (humanState != null && humanState.isFinished()) {
                if (!session.claimPersist()) return;
                int botScore = (int) session.getBotResults().stream().filter(RoundResult::isCorrect).count();
                UUID winnerId = determineWinner(session, humanState.score(), botScore, humanId, session.getPlayerTwoId());
                MatchUpdatedPayload updated = MatchUpdatedPayload.builder()
                        .opponentRounds(toRoundEndPayloads(session.getBotResults()))
                        .opponentScore(botScore)
                        .winnerId(winnerId)
                        .build();
                send(humanId, "MATCH_UPDATED", updated);
                matchPersistenceService.persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
        }, session.getBotFinishAt());
    }

    private void scheduleRoundTimer(MatchSession session, UUID playerId) {
        int timeoutSeconds = appProperties.getMatchmaking().getRoundTimeoutSeconds();
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> processAnswer(playerId, session.getMatchId(), null),
                Instant.now().plusSeconds(timeoutSeconds)
        );
        PlayerRoundState state = session.stateFor(playerId);
        if (state != null) state.setRoundTimer(future);
    }

    private void sendMatchFound(MatchSession session, UUID playerId) {
        String opponentName = getOpponentName(session, playerId);
        MatchFoundPayload payload = MatchFoundPayload.builder()
                .matchId(session.getMatchId())
                .opponentName(opponentName)
                .bot(session.isBot())
                .build();
        send(playerId, "MATCH_FOUND", payload);
    }

    private void sendRoundStart(MatchSession session, UUID playerId) {
        PlayerRoundState state = session.stateFor(playerId);
        if (state == null) return;
        int roundIndex = state.getCurrentRound();
        QuestionEntity question = session.getQuestions().get(roundIndex);

        List<AnswerOption> options = question.getAnswers().stream()
                .map(a -> new AnswerOption(a.getId(), a.getText(), a.getPosition()))
                .toList();

        RoundStartPayload payload = RoundStartPayload.builder()
                .roundNumber(roundIndex + 1)
                .totalRounds(session.getQuestions().size())
                .questionText(question.getText())
                .answers(options)
                .timeoutSeconds(appProperties.getMatchmaking().getRoundTimeoutSeconds())
                .build();
        send(playerId, "ROUND_START", payload);
    }

    private String getOpponentName(MatchSession session, UUID playerId) {
        if (session.isBot()) return "Bot";
        UUID opponentId = session.opponentId(playerId);
        return playerRepository.findById(opponentId)
                .map(PlayerEntity::getName)
                .orElse("Unknown");
    }

    private UUID determineWinner(MatchSession session, int myScore, int oppScore, UUID myId, UUID oppId) {
        if (myScore > oppScore) return myId;
        if (oppScore > myScore) return oppId;
        return null;
    }

    private List<RoundEndPayload> toRoundEndPayloads(List<RoundResult> results) {
        return results.stream().map(r -> RoundEndPayload.builder()
                .roundNumber(r.getRoundNumber())
                .correctAnswerId(r.getCorrectAnswerId())
                .yourAnswerId(r.getAnswerId())
                .correct(r.isCorrect())
                .build()).toList();
    }

    private void send(UUID playerId, String type, Object payload) {
        messagingTemplate.convertAndSend("/topic/player/" + playerId, new WsMessage<>(type, payload));
    }
}
