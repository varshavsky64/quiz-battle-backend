package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.config.AppProperties;
import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.match.MatchSession;
import com.github.varshavsky64.quizbattle.match.RoundResult;
import com.github.varshavsky64.quizbattle.repository.MatchRepository;
import com.github.varshavsky64.quizbattle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final AppProperties appProperties;
    private final QuestionService questionService;
    private final BotService botService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TaskScheduler taskScheduler;

    // playerId -> scheduled bot-match task
    private final ConcurrentLinkedQueue<UUID> randomQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, ScheduledFuture<?>> pendingBotTasks = new ConcurrentHashMap<>();
    private final Map<String, UUID> privateRooms = new ConcurrentHashMap<>(); // code -> playerId

    @Transactional
    public void joinRandomQueue(UUID playerId) {
        // Try to match with waiting player
        UUID opponentId = randomQueue.poll();
        if (opponentId != null && !opponentId.equals(playerId)) {
            cancelBotTask(opponentId);
            createMatch(opponentId, playerId, false);
        } else {
            randomQueue.add(playerId);
            // Schedule bot match after timeout
            int timeoutSeconds = appProperties.getMatchmaking().getQueueTimeoutSeconds();
            ScheduledFuture<?> task = taskScheduler.schedule(
                    () -> startBotMatchIfStillWaiting(playerId),
                    Instant.now().plusSeconds(timeoutSeconds)
            );
            pendingBotTasks.put(playerId, task);
        }
    }

    public String createPrivateRoom(UUID playerId) {
        String code = generateRoomCode();
        privateRooms.put(code, playerId);
        return code;
    }

    @Transactional
    public boolean joinPrivateRoom(UUID playerId, String code) {
        UUID hostId = privateRooms.remove(code);
        if (hostId == null) return false;
        createMatch(hostId, playerId, false);
        return true;
    }

    private void startBotMatchIfStillWaiting(UUID playerId) {
        boolean removed = randomQueue.remove(playerId);
        pendingBotTasks.remove(playerId);
        if (removed) {
            createMatch(playerId, UUID.randomUUID(), true);
        }
    }

    private void cancelBotTask(UUID playerId) {
        ScheduledFuture<?> task = pendingBotTasks.remove(playerId);
        if (task != null) task.cancel(false);
    }

    @Transactional
    protected void createMatch(UUID playerOneId, UUID playerTwoId, boolean bot) {
        int rounds = appProperties.getMatchmaking().getRoundsPerMatch();
        List<QuestionEntity> questions = questionService.selectQuestionsForPlayer(playerOneId, rounds);

        PlayerEntity playerOne = playerRepository.findById(playerOneId).orElseThrow();
        PlayerEntity playerTwo = bot ? createBotPlayer() : playerRepository.findById(playerTwoId).orElseThrow();

        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setPlayerOne(playerOne);
        matchEntity.setPlayerTwo(playerTwo);
        matchEntity.setBot(bot);
        matchEntity = matchRepository.save(matchEntity);

        MatchSession session = new MatchSession(matchEntity.getId(), playerOneId, playerTwoId, bot, questions);

        if (bot) {
            List<RoundResult> botResults = botService.computeBotResults(questions);
            Instant botFinishAt = botService.computeBotFinishTime(rounds);
            session.setBotData(botResults, botFinishAt);
        }

        matchService.startMatch(session);
    }

    private PlayerEntity createBotPlayer() {
        PlayerEntity bot = new PlayerEntity("Bot");
        return playerRepository.save(bot);
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
