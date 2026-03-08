package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MatchEndPayload {
    private List<RoundEndPayload> rounds;
    private List<RoundEndPayload> opponentRounds;
    private int yourScore;
    private int opponentScore;
    private UUID winnerId;
    private boolean opponentFinished;
    private String opponentName;
}
