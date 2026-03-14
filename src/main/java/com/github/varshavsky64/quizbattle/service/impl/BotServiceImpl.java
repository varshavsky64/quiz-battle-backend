package com.github.varshavsky64.quizbattle.service.impl;

import com.github.varshavsky64.quizbattle.config.AppProperties;
import com.github.varshavsky64.quizbattle.domain.entity.AnswerEntity;
import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.match.RoundResult;
import com.github.varshavsky64.quizbattle.service.BotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final AppProperties appProperties;
    private final Random random = new Random();

    @Override
    public List<RoundResult> computeBotResults(List<QuestionEntity> questions) {
        List<RoundResult> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            QuestionEntity question = questions.get(i);
            AppProperties.Bot botConfig = appProperties.getBot();

            boolean answerCorrectly = random.nextDouble() < botConfig.getCorrectAnswerProbability();
            String correctAnswerId = question.getAnswers().stream()
                    .filter(AnswerEntity::isCorrect)
                    .map(AnswerEntity::getId)
                    .findFirst().orElse(null);

            String answerId;
            if (answerCorrectly) {
                answerId = correctAnswerId;
            } else {
                List<AnswerEntity> wrongAnswers = question.getAnswers().stream()
                        .filter(a -> !a.isCorrect())
                        .toList();
                answerId = wrongAnswers.isEmpty() ? null : wrongAnswers.get(random.nextInt(wrongAnswers.size())).getId();
            }

            results.add(new RoundResult(i + 1, question.getId(), answerId, correctAnswerId, answerCorrectly, LocalDateTime.now()));
        }
        return results;
    }

    @Override
    public Instant computeBotFinishTime(int rounds) {
        AppProperties.Bot botConfig = appProperties.getBot();
        int totalDelay = 0;
        for (int i = 0; i < rounds; i++) {
            totalDelay += botConfig.getMinAnswerDelaySeconds() +
                    random.nextInt(botConfig.getMaxAnswerDelaySeconds() - botConfig.getMinAnswerDelaySeconds() + 1);
        }
        return Instant.now().plusSeconds(totalDelay);
    }
}
