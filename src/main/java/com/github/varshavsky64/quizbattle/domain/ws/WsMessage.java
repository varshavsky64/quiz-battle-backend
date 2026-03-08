package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WsMessage<T> {
    private final String type;
    private final T payload;
}
