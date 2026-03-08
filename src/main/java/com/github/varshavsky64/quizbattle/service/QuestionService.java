package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.request.QuestionRequest;
import com.github.varshavsky64.quizbattle.domain.response.QuestionResponse;
import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        QuestionEntity question = new QuestionEntity(request.getText(), request.getDifficulty());
        for (QuestionRequest.AnswerRequest ar : request.getAnswers()) {
            AnswerEntity answer = new AnswerEntity();
            answer.setQuestion(question);
            answer.setText(ar.getText());
            answer.setCorrect(ar.isCorrect());
            answer.setPosition(ar.getPosition());
            question.getAnswers().add(answer);
        }
        return new QuestionResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        QuestionEntity question = findEntityById(id);
        question.setText(request.getText());
        question.setDifficulty(request.getDifficulty());
        question.getAnswers().clear();
        for (QuestionRequest.AnswerRequest ar : request.getAnswers()) {
            AnswerEntity answer = new AnswerEntity();
            answer.setQuestion(question);
            answer.setText(ar.getText());
            answer.setCorrect(ar.isCorrect());
            answer.setPosition(ar.getPosition());
            question.getAnswers().add(answer);
        }
        return new QuestionResponse(questionRepository.save(question));
    }

    @Transactional
    public void delete(Long id) {
        questionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> list(Pageable pageable) {
        return questionRepository.findAll(pageable).map(QuestionResponse::new);
    }

    @Transactional(readOnly = true)
    public List<QuestionEntity> selectQuestionsForPlayer(UUID playerId, int count) {
        List<QuestionEntity> questions = questionRepository.findQuestionsExcludingRecent(playerId, count);
        if (questions.size() < count) {
            // Pool too small, fill from random
            List<QuestionEntity> fallback = questionRepository.findRandom(count);
            for (QuestionEntity q : fallback) {
                if (questions.size() >= count) break;
                if (questions.stream().noneMatch(existing -> existing.getId().equals(q.getId()))) {
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    private QuestionEntity findEntityById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }
}
