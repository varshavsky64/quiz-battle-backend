package com.github.varshavsky64.quizbattle.domain.response;

import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class MatchHistoryResponse {
    private final UUID matchId;
    private final String opponentName;
    private final boolean bot;
    private final UUID winnerId;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;

    public MatchHistoryResponse(MatchEntity entity, UUID currentPlayerId) {
        this.matchId = entity.getId();
        boolean isPlayerOne = entity.getPlayerOne().getId().equals(currentPlayerId);
        this.opponentName = isPlayerOne ? entity.getPlayerTwo().getName() : entity.getPlayerOne().getName();
        this.bot = entity.isBot();
        this.winnerId = entity.getWinner() != null ? entity.getWinner().getId() : null;
        this.startedAt = entity.getStartedAt();
        this.finishedAt = entity.getFinishedAt();
    }
}
