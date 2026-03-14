package com.github.varshavsky64.quizbattle.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinMatchResponse {
    private JoinMatchStatus status;
    private String roomCode;
}
