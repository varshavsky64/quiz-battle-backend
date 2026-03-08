package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.request.CreatePlayerRequest;
import com.github.varshavsky64.quizbattle.domain.response.PlayerResponse;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        PlayerEntity player = new PlayerEntity(request.getName());
        return new PlayerResponse(playerRepository.save(player));
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(UUID id) {
        return playerRepository.findById(id)
                .map(PlayerResponse::new)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
    }
}
