package com.github.varshavsky64.quizbattle.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
    private Type type;
    private String sender;
    private String content;

    public enum Type {
        CHAT,
        JOIN,
        LEAVE
    }
}
