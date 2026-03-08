package com.github.varshavsky64.quizbattle.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AnswerRequest {
    @NotNull
    private UUID matchId;
    @NotNull
    private Integer roundNumber;
    private String answerId; // nullable = timeout/skip
}
