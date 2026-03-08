package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoundEndPayload {
    private int roundNumber;
    private String correctAnswerId;
    private String yourAnswerId;
    private boolean correct;
}
