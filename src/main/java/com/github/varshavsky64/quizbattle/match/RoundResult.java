package com.github.varshavsky64.quizbattle.match;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RoundResult {
    private final int roundNumber;
    private final long questionId;
    private final String answerId; // null if timed out
    private final String correctAnswerId;
    private final boolean correct;
    private final LocalDateTime answeredAt;
}
