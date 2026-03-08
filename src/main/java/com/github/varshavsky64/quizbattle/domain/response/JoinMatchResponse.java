package com.github.varshavsky64.quizbattle.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinMatchResponse {
    private String status; // "WAITING" or "MATCHED"
    private String roomCode; // for private rooms
}
