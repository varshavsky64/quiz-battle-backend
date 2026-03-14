package com.github.varshavsky64.quizbattle.service;

import java.util.UUID;

public interface MatchmakingService {

    void joinRandomQueue(UUID playerId);

    String createPrivateRoom(UUID playerId);

    boolean joinPrivateRoom(UUID playerId, String code);
}
