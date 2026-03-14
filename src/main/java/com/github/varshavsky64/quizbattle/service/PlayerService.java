package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.request.CreatePlayerRequest;
import com.github.varshavsky64.quizbattle.domain.response.PlayerResponse;

import java.util.UUID;

public interface PlayerService {

    PlayerResponse createPlayer(CreatePlayerRequest request);

    PlayerResponse getPlayer(UUID id);
}
