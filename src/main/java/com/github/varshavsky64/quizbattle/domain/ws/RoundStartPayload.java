package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoundStartPayload {
    private int roundNumber;
    private int totalRounds;
    private String questionText;
    private List<AnswerOption> answers;
    private int timeoutSeconds;
}
