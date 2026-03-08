package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MatchUpdatedPayload {
    private List<RoundEndPayload> opponentRounds;
    private int opponentScore;
    private UUID winnerId;
}
