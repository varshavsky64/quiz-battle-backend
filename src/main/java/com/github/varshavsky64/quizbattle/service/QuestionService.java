package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.domain.request.QuestionRequest;
import com.github.varshavsky64.quizbattle.domain.response.QuestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QuestionService {

    QuestionResponse create(QuestionRequest request);

    QuestionResponse update(Long id, QuestionRequest request);

    void delete(Long id);

    Page<QuestionResponse> list(Pageable pageable);

    List<QuestionEntity> selectQuestionsForPlayer(UUID playerId, int count);
}
