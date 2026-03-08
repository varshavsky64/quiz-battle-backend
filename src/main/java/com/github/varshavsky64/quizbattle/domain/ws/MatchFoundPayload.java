package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MatchFoundPayload {
    private UUID matchId;
    private String opponentName;
    private boolean bot;
}
