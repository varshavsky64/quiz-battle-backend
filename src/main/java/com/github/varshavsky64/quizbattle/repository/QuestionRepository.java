package com.github.varshavsky64.quizbattle.repository;

import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    Page<QuestionEntity> findAll(Pageable pageable);

    @Query(value = """
        WITH recent AS (
            SELECT question_id FROM player_question_history
            WHERE player_id = :playerId
            ORDER BY seen_at DESC
            LIMIT 20
        )
        SELECT * FROM questions
        WHERE id NOT IN (SELECT question_id FROM recent)
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<QuestionEntity> findQuestionsExcludingRecent(@Param("playerId") UUID playerId, @Param("limit") int limit);

    @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<QuestionEntity> findRandom(@Param("limit") int limit);
}
