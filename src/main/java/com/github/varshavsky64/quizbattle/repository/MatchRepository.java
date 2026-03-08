package com.github.varshavsky64.quizbattle.repository;

import com.github.varshavsky64.quizbattle.domain.entity.MatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    Page<MatchEntity> findByPlayerOneIdOrPlayerTwoIdOrderByStartedAtDesc(UUID playerOneId, UUID playerTwoId, Pageable pageable);
}
