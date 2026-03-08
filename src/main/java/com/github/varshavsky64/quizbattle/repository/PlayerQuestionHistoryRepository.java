package com.github.varshavsky64.quizbattle.repository;

import com.github.varshavsky64.quizbattle.domain.entity.PlayerQuestionHistoryEntity;
import com.github.varshavsky64.quizbattle.domain.entity.PlayerQuestionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerQuestionHistoryRepository extends JpaRepository<PlayerQuestionHistoryEntity, PlayerQuestionHistoryId> {
}
