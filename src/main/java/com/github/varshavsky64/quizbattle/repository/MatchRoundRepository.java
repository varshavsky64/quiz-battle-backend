package com.github.varshavsky64.quizbattle.repository;

import com.github.varshavsky64.quizbattle.domain.entity.MatchRoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRoundRepository extends JpaRepository<MatchRoundEntity, Long> {

    List<MatchRoundEntity> findByMatchIdAndPlayerIdOrderByRoundNumber(UUID matchId, UUID playerId);

    List<MatchRoundEntity> findByMatchIdOrderByRoundNumber(UUID matchId);
}
