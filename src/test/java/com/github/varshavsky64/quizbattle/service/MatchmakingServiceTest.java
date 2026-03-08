package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.config.AppProperties;
import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.repository.MatchRepository;
import com.github.varshavsky64.quizbattle.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(MatchmakingService.class)
@DisplayName("Private room creation and random matchmaking")
class MatchmakingServiceTest {

    @MockitoBean AppProperties appProperties;
    @MockitoBean QuestionService questionService;
    @MockitoBean BotService botService;
    @MockitoBean MatchService matchService;
    @MockitoBean MatchRepository matchRepository;
    @MockitoBean PlayerRepository playerRepository;
    @MockitoBean TaskScheduler taskScheduler;
    @MockitoBean ScheduledFuture<?> scheduledFuture;

    @Autowired
    MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        AppProperties.Matchmaking matchmakingConfig = new AppProperties.Matchmaking();
        matchmakingConfig.setQueueTimeoutSeconds(10);
        matchmakingConfig.setRoundsPerMatch(5);
        matchmakingConfig.setRoundTimeoutSeconds(10);
        when(appProperties.getMatchmaking()).thenReturn(matchmakingConfig);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Returns a 6-character alphanumeric code")
    void createPrivateRoom_returnsCode() {
        // when
        String code = matchmakingService.createPrivateRoom(UUID.randomUUID());

        // then
        assertThat(code).isNotBlank();
        assertThat(code).hasSize(6);
        assertThat(code).matches("[A-Z0-9]{6}");
    }

    @Test
    @DisplayName("Returns a unique code on each call")
    void createPrivateRoom_eachCallReturnsUniqueCode() {
        // when
        String code1 = matchmakingService.createPrivateRoom(UUID.randomUUID());
        String code2 = matchmakingService.createPrivateRoom(UUID.randomUUID());

        // then
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("Starts a match and returns true when code is valid")
    void joinPrivateRoom_withValidCode_returnsTrue() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        String code = matchmakingService.createPrivateRoom(hostId);
        setupMatchCreationMocks(hostId, guestId);

        // when
        boolean joined = matchmakingService.joinPrivateRoom(guestId, code);

        // then
        assertThat(joined).isTrue();
        verify(matchService).startMatch(any());
    }

    @Test
    @DisplayName("Returns false and does not start a match when code is invalid")
    void joinPrivateRoom_withInvalidCode_returnsFalse() {
        // when
        boolean joined = matchmakingService.joinPrivateRoom(UUID.randomUUID(), "BADCOD");

        // then
        assertThat(joined).isFalse();
        verifyNoInteractions(matchService);
    }

    @Test
    @DisplayName("Code becomes invalid after first use")
    void joinPrivateRoom_codeIsConsumedAfterUse() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID guest1 = UUID.randomUUID();
        String code = matchmakingService.createPrivateRoom(hostId);
        setupMatchCreationMocks(hostId, guest1);
        matchmakingService.joinPrivateRoom(guest1, code);

        // when
        boolean secondJoin = matchmakingService.joinPrivateRoom(UUID.randomUUID(), code);

        // then
        assertThat(secondJoin).isFalse();
    }

    @Test
    @DisplayName("Schedules a bot match when no opponent is waiting")
    void joinRandom_firstPlayer_schedulesBot() {
        // when
        matchmakingService.joinRandomQueue(UUID.randomUUID());

        // then
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        verifyNoInteractions(matchService);
    }

    @Test
    @DisplayName("Matches two players immediately without a bot")
    void joinRandom_twoPlayers_matchedImmediately() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        setupMatchCreationMocks(player1, player2);

        // when
        matchmakingService.joinRandomQueue(player1);
        matchmakingService.joinRandomQueue(player2);

        // then
        verify(matchService).startMatch(any());
    }

    private void setupMatchCreationMocks(UUID playerOneId, UUID playerTwoId) {
        List<QuestionEntity> questions = new ArrayList<>(List.of(
                new QuestionEntity("Q1", (short) 1), new QuestionEntity("Q2", (short) 1),
                new QuestionEntity("Q3", (short) 1), new QuestionEntity("Q4", (short) 1),
                new QuestionEntity("Q5", (short) 1)
        ));
        when(questionService.selectQuestionsForPlayer(any(), anyInt())).thenReturn(questions);
        when(playerRepository.findById(playerOneId)).thenReturn(Optional.of(new PlayerEntity("Player1")));
        when(playerRepository.findById(playerTwoId)).thenReturn(Optional.of(new PlayerEntity("Player2")));

        MatchEntity matchEntity = new MatchEntity();
        try {
            var field = MatchEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(matchEntity, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        matchEntity.setPlayerOne(new PlayerEntity("Player1"));
        matchEntity.setPlayerTwo(new PlayerEntity("Player2"));
        when(matchRepository.save(any())).thenReturn(matchEntity);
    }
}
