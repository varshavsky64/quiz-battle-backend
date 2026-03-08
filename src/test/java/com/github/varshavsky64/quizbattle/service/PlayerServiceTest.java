package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.request.CreatePlayerRequest;
import com.github.varshavsky64.quizbattle.domain.response.PlayerResponse;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import(PlayerService.class)
@DisplayName("Player creation and retrieval")
class PlayerServiceTest {

    @MockitoBean
    PlayerRepository playerRepository;

    @Autowired
    PlayerService playerService;

    @Test
    @DisplayName("Saves player and returns correct response")
    void createPlayer_savesAndReturnsResponse() {
        // given
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName("Alice");
        PlayerEntity saved = playerWithId(UUID.randomUUID(), "Alice");
        when(playerRepository.save(any())).thenReturn(saved);

        // when
        PlayerResponse response = playerService.createPlayer(request);

        // then
        ArgumentCaptor<PlayerEntity> captor = ArgumentCaptor.forClass(PlayerEntity.class);
        verify(playerRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getWins()).isZero();
        assertThat(response.getLosses()).isZero();
    }

    @Test
    @DisplayName("Returns player response when player exists")
    void getPlayer_returnsResponse_whenFound() {
        // given
        UUID id = UUID.randomUUID();
        when(playerRepository.findById(id)).thenReturn(Optional.of(playerWithId(id, "Bob")));

        // when
        PlayerResponse response = playerService.getPlayer(id);

        // then
        assertThat(response.getName()).isEqualTo("Bob");
        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Throws exception when player is not found")
    void getPlayer_throws_whenNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(playerRepository.findById(id)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> playerService.getPlayer(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(id.toString());
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
