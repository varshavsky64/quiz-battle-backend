package com.github.varshavsky64.quizbattle.controller;

import com.github.varshavsky64.quizbattle.domain.response.JoinMatchResponse;
import com.github.varshavsky64.quizbattle.domain.response.JoinMatchStatus;
import com.github.varshavsky64.quizbattle.domain.response.MatchHistoryResponse;
import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.service.MatchService;
import com.github.varshavsky64.quizbattle.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchmakingService matchmakingService;
    private final MatchService matchService;

    @PostMapping("/random")
    public JoinMatchResponse joinRandom(@RequestParam UUID playerId) {
        matchmakingService.joinRandomQueue(playerId);
        return new JoinMatchResponse(JoinMatchStatus.WAITING, null);
    }

    @PostMapping("/private")
    public JoinMatchResponse createPrivate(@RequestParam UUID playerId) {
        String code = matchmakingService.createPrivateRoom(playerId);
        return new JoinMatchResponse(JoinMatchStatus.WAITING, code);
    }

    @PostMapping("/private/{code}/join")
    public JoinMatchResponse joinPrivate(@PathVariable String code, @RequestParam UUID playerId) {
        boolean joined = matchmakingService.joinPrivateRoom(playerId, code);
        return new JoinMatchResponse(joined ? JoinMatchStatus.MATCHED : JoinMatchStatus.NOT_FOUND, null);
    }

    @GetMapping("/{id}")
    public MatchEntity getMatch(@PathVariable UUID id) {
        return matchService.getMatch(id);
    }

    @GetMapping("/player/{playerId}")
    public Page<MatchHistoryResponse> playerHistory(
            @PathVariable UUID playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return matchService.getPlayerHistory(playerId, page, size);
    }
}
