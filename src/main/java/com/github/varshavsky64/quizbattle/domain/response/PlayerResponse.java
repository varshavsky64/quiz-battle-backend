package com.github.varshavsky64.quizbattle.domain.response;

import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PlayerResponse {
    private final UUID id;
    private final String name;
    private final int wins;
    private final int losses;

    public PlayerResponse(PlayerEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.wins = entity.getWins();
        this.losses = entity.getLosses();
    }
}
