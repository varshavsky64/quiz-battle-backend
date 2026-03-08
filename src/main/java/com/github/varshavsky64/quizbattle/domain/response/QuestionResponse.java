package com.github.varshavsky64.quizbattle.domain.response;

import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import lombok.Getter;

import java.util.List;

@Getter
public class QuestionResponse {
    private final Long id;
    private final String text;
    private final short difficulty;
    private final List<AnswerResponse> answers;

    public QuestionResponse(QuestionEntity entity) {
        this.id = entity.getId();
        this.text = entity.getText();
        this.difficulty = entity.getDifficulty();
        this.answers = entity.getAnswers().stream().map(AnswerResponse::new).toList();
    }

    @Getter
    public static class AnswerResponse {
        private final String id;
        private final String text;
        private final boolean correct;
        private final short position;

        public AnswerResponse(AnswerEntity entity) {
            this.id = entity.getId();
            this.text = entity.getText();
            this.correct = entity.isCorrect();
            this.position = entity.getPosition();
        }
    }
}
