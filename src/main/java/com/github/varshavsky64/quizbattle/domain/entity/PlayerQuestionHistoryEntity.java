package com.github.varshavsky64.quizbattle.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_question_history")
@IdClass(PlayerQuestionHistoryId.class)
public class PlayerQuestionHistoryEntity {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt = LocalDateTime.now();

    public PlayerQuestionHistoryEntity(UUID playerId, Long questionId) {
        this.playerId = playerId;
        this.questionId = questionId;
    }
}
