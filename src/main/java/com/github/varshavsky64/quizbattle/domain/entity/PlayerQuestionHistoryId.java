package com.github.varshavsky64.quizbattle.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlayerQuestionHistoryId implements Serializable {
    private UUID playerId;
    private Long questionId;
}
