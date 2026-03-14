package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.match.RoundResult;

import java.time.Instant;
import java.util.List;

public interface BotService {

    List<RoundResult> computeBotResults(List<QuestionEntity> questions);

    Instant computeBotFinishTime(int rounds);
}
