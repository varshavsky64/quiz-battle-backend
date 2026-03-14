package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.match.MatchSession;

public interface MatchPersistenceService {

    void persistMatch(MatchSession session);
}
