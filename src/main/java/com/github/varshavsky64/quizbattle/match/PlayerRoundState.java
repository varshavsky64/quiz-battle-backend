package com.github.varshavsky64.quizbattle.match;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Getter
public class PlayerRoundState {

    private final List<RoundResult> results = new ArrayList<>();
    private int currentRound = 0; // 0-based index into questions list
    private boolean finished = false;
    private LocalDateTime finishedAt;
    private ScheduledFuture<?> roundTimer;

    public void setRoundTimer(ScheduledFuture<?> timer) {
        this.roundTimer = timer;
    }

    public void cancelTimer() {
        if (roundTimer != null && !roundTimer.isDone()) {
            roundTimer.cancel(false);
        }
    }

    public void addResult(RoundResult result) {
        results.add(result);
        currentRound++;
    }

    public void markFinished() {
        this.finished = true;
        this.finishedAt = LocalDateTime.now();
    }

    public int score() {
        return (int) results.stream().filter(RoundResult::isCorrect).count();
    }
}
