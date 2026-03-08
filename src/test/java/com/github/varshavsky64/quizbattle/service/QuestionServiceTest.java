package com.github.varshavsky64.quizbattle.service;

import com.github.varshavsky64.quizbattle.domain.request.QuestionRequest;
import com.github.varshavsky64.quizbattle.domain.response.QuestionResponse;
import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(QuestionService.class)
@DisplayName("Question management and selection for players")
class QuestionServiceTest {

    @MockitoBean
    QuestionRepository questionRepository;

    @Autowired
    QuestionService questionService;

    @Test
    @DisplayName("Saves question with all answers")
    void create_savesQuestionWithAnswers() {
        // given
        QuestionEntity saved = makeQuestionEntity(1L, "What is 2+2?");
        when(questionRepository.save(any())).thenReturn(saved);

        // when
        QuestionResponse response = questionService.create(buildRequest("What is 2+2?"));

        // then
        ArgumentCaptor<QuestionEntity> captor = ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionRepository).save(captor.capture());
        QuestionEntity captured = captor.getValue();
        assertThat(captured.getText()).isEqualTo("What is 2+2?");
        assertThat(captured.getDifficulty()).isEqualTo((short) 1);
        assertThat(captured.getAnswers()).hasSize(4);
        assertThat(captured.getAnswers().stream().filter(AnswerEntity::isCorrect)).hasSize(1);
        assertThat(response.getText()).isEqualTo("What is 2+2?");
    }

    @Test
    @DisplayName("Replaces text and answers of existing question")
    void update_replacesAnswers() {
        // given
        QuestionEntity existing = makeQuestionEntity(1L, "Old?");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        QuestionResponse response = questionService.update(1L, buildRequest("New?"));

        // then
        assertThat(response.getText()).isEqualTo("New?");
        assertThat(existing.getAnswers()).hasSize(4);
    }

    @Test
    @DisplayName("Throws exception when question is not found")
    void update_throwsWhenNotFound() {
        // given
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> questionService.update(99L, buildRequest("X?")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Delegates deletion to repository by ID")
    void delete_callsRepository() {
        // when
        questionService.delete(5L);

        // then
        verify(questionRepository).deleteById(5L);
    }

    @Test
    @DisplayName("Returns page of questions mapped to responses")
    void list_returnsPage() {
        // given
        Page<QuestionEntity> page = new PageImpl<>(List.of(makeQuestionEntity(1L, "Q?")));
        when(questionRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // when
        Page<QuestionResponse> result = questionService.list(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getText()).isEqualTo("Q?");
    }

    @Test
    @DisplayName("Returns questions from pool when pool is sufficient")
    void selectQuestionsForPlayer_returnsFromPool_whenSufficient() {
        // given
        UUID playerId = UUID.randomUUID();
        List<QuestionEntity> questions = List.of(
                makeQuestionEntity(1L, "Q1"), makeQuestionEntity(2L, "Q2"),
                makeQuestionEntity(3L, "Q3"), makeQuestionEntity(4L, "Q4"),
                makeQuestionEntity(5L, "Q5")
        );
        when(questionRepository.findQuestionsExcludingRecent(playerId, 5)).thenReturn(questions);

        // when
        List<QuestionEntity> result = questionService.selectQuestionsForPlayer(playerId, 5);

        // then
        assertThat(result).hasSize(5);
        verify(questionRepository, never()).findRandom(anyInt());
    }

    @Test
    @DisplayName("Fills from random questions when pool is too small")
    void selectQuestionsForPlayer_fillsFromRandom_whenPoolTooSmall() {
        // given
        UUID playerId = UUID.randomUUID();
        List<QuestionEntity> fromPool = new ArrayList<>(List.of(
                makeQuestionEntity(1L, "Q1"), makeQuestionEntity(2L, "Q2")
        ));
        List<QuestionEntity> fromRandom = List.of(
                makeQuestionEntity(1L, "Q1"), makeQuestionEntity(2L, "Q2"),
                makeQuestionEntity(3L, "Q3"), makeQuestionEntity(4L, "Q4"),
                makeQuestionEntity(5L, "Q5")
        );
        when(questionRepository.findQuestionsExcludingRecent(playerId, 5)).thenReturn(fromPool);
        when(questionRepository.findRandom(5)).thenReturn(fromRandom);

        // when
        List<QuestionEntity> result = questionService.selectQuestionsForPlayer(playerId, 5);

        // then
        assertThat(result).hasSize(5);
        assertThat(result.stream().map(QuestionEntity::getId).distinct()).hasSize(5);
    }

    // --- helpers ---

    private QuestionRequest buildRequest(String text) {
        QuestionRequest request = new QuestionRequest();
        request.setText(text);
        request.setDifficulty((short) 1);
        request.setAnswers(List.of(
                buildAnswer("Correct", true, (short) 0),
                buildAnswer("Wrong A", false, (short) 1),
                buildAnswer("Wrong B", false, (short) 2),
                buildAnswer("Wrong C", false, (short) 3)
        ));
        return request;
    }

    private QuestionRequest.AnswerRequest buildAnswer(String text, boolean correct, short position) {
        QuestionRequest.AnswerRequest a = new QuestionRequest.AnswerRequest();
        a.setText(text);
        a.setCorrect(correct);
        a.setPosition(position);
        return a;
    }

    private QuestionEntity makeQuestionEntity(Long id, String text) {
        QuestionEntity entity = new QuestionEntity(text, (short) 1);
        try {
            var field = QuestionEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AnswerEntity correct = new AnswerEntity();
        correct.setQuestion(entity);
        correct.setText("Correct");
        correct.setCorrect(true);
        correct.setPosition((short) 0);
        AnswerEntity wrong = new AnswerEntity();
        wrong.setQuestion(entity);
        wrong.setText("Wrong");
        wrong.setCorrect(false);
        wrong.setPosition((short) 1);
        entity.getAnswers().addAll(List.of(correct, wrong));
        return entity;
    }
}
