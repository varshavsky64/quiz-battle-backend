package com.github.varshavsky64.quizbattle.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PlayerSessionInterceptor implements ChannelInterceptor {

    private final Map<String, UUID> sessionToPlayer = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String playerIdStr = accessor.getFirstNativeHeader("playerId");
            if (playerIdStr != null) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    String sessionId = accessor.getSessionId();
                    sessionToPlayer.put(sessionId, playerId);
                    Map<String, Object> attrs = accessor.getSessionAttributes();
                    if (attrs != null) attrs.put("playerId", playerId);
                    log.debug("Player {} connected with session {}", playerId, sessionId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid playerId header: {}", playerIdStr);
                }
            }
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            sessionToPlayer.remove(accessor.getSessionId());
        }

        return message;
    }

}
