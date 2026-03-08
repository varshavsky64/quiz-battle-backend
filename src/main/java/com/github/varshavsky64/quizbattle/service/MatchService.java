package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.config.AppProperties;
import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.domain.entity.MatchRoundEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerQuestionHistoryEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.domain.ws.AnswerOption;
import com.github.varshavsky64.quizbattle.domain.ws.MatchEndPayload;
import com.github.varshavsky64.quizbattle.domain.ws.MatchFoundPayload;
import com.github.varshavsky64.quizbattle.domain.ws.MatchUpdatedPayload;
import com.github.varshavsky64.quizbattle.domain.ws.RoundEndPayload;
import com.github.varshavsky64.quizbattle.domain.ws.RoundStartPayload;
import com.github.varshavsky64.quizbattle.domain.ws.WsMessage;
import com.github.varshavsky64.quizbattle.match.MatchSession;
import com.github.varshavsky64.quizbattle.match.PlayerRoundState;
import com.github.varshavsky64.quizbattle.match.RoundResult;
import com.github.varshavsky64.quizbattle.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final AppProperties appProperties;
    private final MatchRepository matchRepository;
    private final MatchRoundRepository matchRoundRepository;
    private final PlayerRepository playerRepository;
    private final PlayerQuestionHistoryRepository historyRepository;

    private final Map<UUID, MatchSession> activeSessions = new ConcurrentHashMap<>();

    public void startMatch(MatchSession session) {
        activeSessions.put(session.getMatchId(), session);

        // Notify both human players
        sendMatchFound(session, session.getPlayerOneId());
        if (!session.isBot()) {
            sendMatchFound(session, session.getPlayerTwoId());
        }

        // Start round 1 for human player(s)
        sendRoundStart(session, session.getPlayerOneId());
        if (!session.isBot()) {
            sendRoundStart(session, session.getPlayerTwoId());
        }

        // Schedule round timer for player one
        scheduleRoundTimer(session, session.getPlayerOneId());
        if (!session.isBot()) {
            scheduleRoundTimer(session, session.getPlayerTwoId());
        }

        // If bot: schedule MATCH_UPDATED notification
        if (session.isBot()) {
            scheduleBotFinish(session);
        }
    }

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

        // Cancel the timer for this round
        state.cancelTimer();

        QuestionEntity question = session.getQuestions().get(roundIndex);
        String correctAnswerId = question.getAnswers().stream()
                .filter(AnswerEntity::isCorrect)
                .map(AnswerEntity::getId)
                .findFirst().orElse(null);

        boolean correct = answerId != null && answerId.equals(correctAnswerId);
        RoundResult result = new RoundResult(roundIndex + 1, question.getId(), answerId, correctAnswerId, correct, java.time.LocalDateTime.now());
        state.addResult(result);

        // Send ROUND_END to this player
        RoundEndPayload roundEnd = RoundEndPayload.builder()
                .roundNumber(roundIndex + 1)
                .correctAnswerId(correctAnswerId)
                .yourAnswerId(answerId)
                .correct(correct)
                .build();
        send(playerId, "ROUND_END", roundEnd);

        // Check if player finished all rounds
        if (state.getCurrentRound() >= session.getQuestions().size()) {
            state.markFinished();
            finishPlayerPart(session, playerId);
        } else {
            // Auto-advance to next round after 2 seconds
            taskScheduler.schedule(
                    () -> {
                        sendRoundStart(session, playerId);
                        scheduleRoundTimer(session, playerId);
                    },
                    Instant.now().plusSeconds(2)
            );
        }
    }

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

        // If opponent already finished, notify opponent with MATCH_UPDATED
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
                persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
        } else if (session.isBot() && opponentFinished) {
            if (session.claimPersist()) {
                persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
        }
    }

    private void scheduleBotFinish(MatchSession session) {
        Instant botFinishAt = session.getBotFinishAt();
        taskScheduler.schedule(() -> {
            // Find which player is the human
            UUID humanId = session.getPlayerOneId();
            PlayerRoundState humanState = session.stateFor(humanId);
            if (humanState != null && humanState.isFinished()) {
                // Human already got MATCH_END but without opponent results; send MATCH_UPDATED
                // claimPersist() is atomic — ensures only one thread ever calls persistMatch
                if (!session.claimPersist()) return;
                int botScore = (int) session.getBotResults().stream().filter(RoundResult::isCorrect).count();
                UUID winnerId = determineWinner(session, humanState.score(), botScore, humanId, session.getPlayerTwoId());
                MatchUpdatedPayload updated = MatchUpdatedPayload.builder()
                        .opponentRounds(toRoundEndPayloads(session.getBotResults()))
                        .opponentScore(botScore)
                        .winnerId(winnerId)
                        .build();
                send(humanId, "MATCH_UPDATED", updated);
                persistMatch(session);
                activeSessions.remove(session.getMatchId());
            }
            // If human hasn't finished yet, they'll get the full result when they do finish
        }, botFinishAt);
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
        return null; // draw
    }

    @Transactional
    protected void persistMatch(MatchSession session) {
        try {
            MatchEntity match = matchRepository.findById(session.getMatchId()).orElseThrow();
            match.setFinishedAt(java.time.LocalDateTime.now());

            // Load players directly to avoid LazyInitializationException from self-invocation
            UUID humanId = session.getPlayerOneId();
            UUID opponentId = session.getPlayerTwoId();
            PlayerEntity playerOne = playerRepository.findById(humanId).orElseThrow();
            PlayerEntity playerTwo = session.isBot() ? null : playerRepository.findById(opponentId).orElseThrow();

            // Determine scores
            PlayerRoundState humanState = session.stateFor(humanId);
            int humanScore = humanState != null ? humanState.score() : 0;
            int botOrOppScore;

            if (session.isBot()) {
                botOrOppScore = (int) session.getBotResults().stream().filter(RoundResult::isCorrect).count();
            } else {
                PlayerRoundState oppState = session.stateFor(opponentId);
                botOrOppScore = oppState != null ? oppState.score() : 0;
            }

            // Set winner
            if (humanScore > botOrOppScore) {
                match.setWinner(playerOne);
            } else if (botOrOppScore > humanScore) {
                if (!session.isBot()) match.setWinner(playerTwo);
            }
            matchRepository.save(match);

            // Update player stats
            if (match.getWinner() != null) {
                if (match.getWinner().getId().equals(playerOne.getId())) {
                    playerOne.setWins(playerOne.getWins() + 1);
                    if (!session.isBot()) playerTwo.setLosses(playerTwo.getLosses() + 1);
                } else {
                    playerOne.setLosses(playerOne.getLosses() + 1);
                    if (!session.isBot()) playerTwo.setWins(playerTwo.getWins() + 1);
                }
            }
            playerRepository.save(playerOne);
            if (!session.isBot()) playerRepository.save(playerTwo);

            // Save rounds for human player
            saveRounds(match, playerOne, humanState != null ? humanState.getResults() : List.of(), session.getQuestions());

            // Save rounds for opponent (bot has no player entity in DB, so skip)
            if (!session.isBot()) {
                PlayerRoundState oppState = session.stateFor(opponentId);
                saveRounds(match, playerTwo, oppState != null ? oppState.getResults() : List.of(), session.getQuestions());
            }

            // Update question history for human
            if (humanState != null) {
                for (QuestionEntity q : session.getQuestions()) {
                    PlayerQuestionHistoryEntity hist = new PlayerQuestionHistoryEntity(humanId, q.getId());
                    historyRepository.save(hist);
                }
            }
        } catch (Exception e) {
            log.error("Failed to persist match {}", session.getMatchId(), e);
        }
    }

    private void saveRounds(MatchEntity match, PlayerEntity player, List<RoundResult> results, List<QuestionEntity> questions) {
        for (RoundResult result : results) {
            MatchRoundEntity round = new MatchRoundEntity();
            round.setMatch(match);
            round.setPlayer(player);
            round.setRoundNumber((short) result.getRoundNumber());
            round.setQuestion(questions.stream()
                    .filter(q -> q.getId().equals(result.getQuestionId()))
                    .findFirst().orElse(null));
            round.setCorrect(result.isCorrect());
            round.setAnsweredAt(result.getAnsweredAt());
            matchRoundRepository.save(round);
        }
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
