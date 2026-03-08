package com.github.varshavsky64.quizbattle.repository;

import com.github.varshavsky64.quizbattle.domain.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
}
