package com.github.varshavsky64.quizbattle.controller;

import com.github.varshavsky64.quizbattle.domain.request.AnswerRequest;
import com.github.varshavsky64.quizbattle.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchWebSocketController {

    private final MatchService matchService;

    @MessageMapping("/match/answer")
    public void handleAnswer(@Payload @Valid AnswerRequest request, SimpMessageHeaderAccessor headerAccessor) {
        UUID playerId = getPlayerId(headerAccessor);
        if (playerId == null) return;
        matchService.processAnswer(playerId, request.getMatchId(), request.getAnswerId());
    }

    @MessageMapping("/match/next")
    public void handleNext(@Payload AnswerRequest request, SimpMessageHeaderAccessor headerAccessor) {
        UUID playerId = getPlayerId(headerAccessor);
        if (playerId == null) return;
        matchService.requestNextRound(playerId, request.getMatchId());
    }

    private UUID getPlayerId(SimpMessageHeaderAccessor headerAccessor) {
        Object val = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("playerId")
                : null;
        if (val instanceof UUID id) return id;
        log.warn("No playerId in session attributes");
        return null;
    }
}
