package com.github.varshavsky64.quizbattle.controller;

import com.github.varshavsky64.quizbattle.domain.request.CreatePlayerRequest;
import com.github.varshavsky64.quizbattle.domain.response.PlayerResponse;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
@DisplayName("Player registration and retrieval")
class PlayerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PlayerService playerService;

    @Test
    @DisplayName("Creates a player and returns 201 with player data")
    void create_returns201WithPlayer() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        PlayerResponse response = new PlayerResponse(playerWithId(id, "Alice"));
        when(playerService.createPlayer(any())).thenReturn(response);

        // when
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithName("Alice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.wins").value(0))
                .andExpect(jsonPath("$.losses").value(0));
    }

    @Test
    @DisplayName("Returns 400 when name is too short")
    void create_returns400_whenNameTooShort() throws Exception {
        // when
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithName("A"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Returns 400 when name is blank")
    void create_returns400_whenNameBlank() throws Exception {
        // when
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithName("  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Returns player data by ID")
    void getById_returnsPlayer() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        PlayerResponse response = new PlayerResponse(playerWithId(id, "Bob"));
        when(playerService.getPlayer(id)).thenReturn(response);

        // when
        mockMvc.perform(get("/api/v1/players/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.id").value(id.toString()));

        // then
        verify(playerService).getPlayer(id);
    }

    private CreatePlayerRequest requestWithName(String name) {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName(name);
        return request;
    }

    private PlayerEntity playerWithId(UUID id, String name) {
        PlayerEntity entity = new PlayerEntity(name);
        try {
            var field = PlayerEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
