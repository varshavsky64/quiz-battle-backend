package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.domain.response.MatchHistoryResponse;
import com.github.varshavsky64.quizbattle.match.MatchSession;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MatchService {

    MatchEntity getMatch(UUID id);

    Page<MatchHistoryResponse> getPlayerHistory(UUID playerId, int page, int size);

    void startMatch(MatchSession session);

    void processAnswer(UUID playerId, UUID matchId, String answerId);

    void requestNextRound(UUID playerId, UUID matchId);
}
