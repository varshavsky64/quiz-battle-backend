package com.github.varshavsky64.quizbattle.controller;

import com.github.varshavsky64.quizbattle.domain.request.CreatePlayerRequest;
import com.github.varshavsky64.quizbattle.domain.response.PlayerResponse;
import com.github.varshavsky64.quizbattle.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse create(@RequestBody @Valid CreatePlayerRequest request) {
        return playerService.createPlayer(request);
    }

    @GetMapping("/{id}")
    public PlayerResponse get(@PathVariable UUID id) {
        return playerService.getPlayer(id);
    }
}
