package com.github.varshavsky64.quizbattle.match;

import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class MatchSession {

    private final UUID matchId;
    private final UUID playerOneId;
    private final UUID playerTwoId; // may be a fake UUID for bot
    private final boolean bot;
    private final List<QuestionEntity> questions;
    private final Map<UUID, PlayerRoundState> playerStates = new ConcurrentHashMap<>();

    // Bot pre-computed results (only relevant when bot=true)
    private List<RoundResult> botResults;
    private Instant botFinishAt;
    private final AtomicBoolean persisted = new AtomicBoolean(false);

    /** Returns true exactly once — whichever thread wins gets to call persistMatch. */
    public boolean claimPersist() {
        return persisted.compareAndSet(false, true);
    }

    public MatchSession(UUID matchId, UUID playerOneId, UUID playerTwoId, boolean bot, List<QuestionEntity> questions) {
        this.matchId = matchId;
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
        this.bot = bot;
        this.questions = questions;
        playerStates.put(playerOneId, new PlayerRoundState());
        if (!bot) {
            playerStates.put(playerTwoId, new PlayerRoundState());
        }
    }

    public void setBotData(List<RoundResult> botResults, Instant botFinishAt) {
        this.botResults = botResults;
        this.botFinishAt = botFinishAt;
    }

    public PlayerRoundState stateFor(UUID playerId) {
        return playerStates.get(playerId);
    }

    public UUID opponentId(UUID playerId) {
        return playerId.equals(playerOneId) ? playerTwoId : playerOneId;
    }

    public boolean bothFinished() {
        if (bot) {
            PlayerRoundState humanState = playerStates.get(playerOneId);
            if (humanState == null) humanState = playerStates.get(playerTwoId);
            return humanState != null && humanState.isFinished() && Instant.now().isAfter(botFinishAt);
        }
        return playerStates.values().stream().allMatch(PlayerRoundState::isFinished);
    }

    public boolean isHumanPlayer(UUID playerId) {
        return !bot || playerId.equals(playerOneId) || playerId.equals(playerTwoId);
    }
}
