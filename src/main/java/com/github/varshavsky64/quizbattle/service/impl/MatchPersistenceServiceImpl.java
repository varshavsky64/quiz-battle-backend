package com.github.varshavsky64.quizbattle.service.impl;

import com.github.varshavsky64.quizbattle.domain.entity.*;
import com.github.varshavsky64.quizbattle.match.MatchSession;
import com.github.varshavsky64.quizbattle.match.PlayerRoundState;
import com.github.varshavsky64.quizbattle.match.RoundResult;
import com.github.varshavsky64.quizbattle.repository.*;
import com.github.varshavsky64.quizbattle.service.MatchPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchPersistenceServiceImpl implements MatchPersistenceService {

    private final MatchRepository matchRepository;
    private final MatchRoundRepository matchRoundRepository;
    private final PlayerRepository playerRepository;
    private final PlayerQuestionHistoryRepository historyRepository;

    @Override
    @Transactional
    public void persistMatch(MatchSession session) {
        try {
            MatchEntity match = matchRepository.findById(session.getMatchId()).orElseThrow();
            match.setFinishedAt(LocalDateTime.now());

            UUID humanId = session.getPlayerOneId();
            UUID opponentId = session.getPlayerTwoId();
            PlayerEntity playerOne = match.getPlayerOne();
            PlayerEntity playerTwo = match.getPlayerTwo();

            PlayerRoundState humanState = session.stateFor(humanId);
            int humanScore = humanState != null ? humanState.score() : 0;
            int botOrOppScore;

            if (session.isBot()) {
                botOrOppScore = (int) session.getBotResults().stream().filter(RoundResult::isCorrect).count();
            } else {
                PlayerRoundState oppState = session.stateFor(opponentId);
                botOrOppScore = oppState != null ? oppState.score() : 0;
            }

            if (humanScore > botOrOppScore) {
                match.setWinner(playerOne);
                playerOne.setWins(playerOne.getWins() + 1);
                if (!session.isBot()) playerTwo.setLosses(playerTwo.getLosses() + 1);
            } else if (botOrOppScore > humanScore) {
                match.setWinner(playerTwo);
                playerOne.setLosses(playerOne.getLosses() + 1);
                if (!session.isBot()) playerTwo.setWins(playerTwo.getWins() + 1);
            }
            matchRepository.save(match);
            playerRepository.save(playerOne);
            if (!session.isBot()) playerRepository.save(playerTwo);

            saveRounds(match, playerOne, humanState != null ? humanState.getResults() : List.of(), session.getQuestions());

            if (!session.isBot()) {
                PlayerRoundState oppState = session.stateFor(opponentId);
                saveRounds(match, playerTwo, oppState != null ? oppState.getResults() : List.of(), session.getQuestions());
            }

            if (humanState != null) {
                for (QuestionEntity q : session.getQuestions()) {
                    historyRepository.save(new PlayerQuestionHistoryEntity(humanId, q.getId()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to persist match {}", session.getMatchId(), e);
        }
    }

    private void saveRounds(MatchEntity match, PlayerEntity player, List<RoundResult> results, List<QuestionEntity> questions) {
        for (RoundResult result : results) {
            MatchRoundEntity round = new MatchRoundEntity();
            round.setMatch(match);
            round.setPlayer(player);
            round.setRoundNumber((short) result.getRoundNumber());
            round.setQuestion(questions.stream()
                    .filter(q -> q.getId().equals(result.getQuestionId()))
                    .findFirst().orElse(null));
            round.setCorrect(result.isCorrect());
            round.setAnsweredAt(result.getAnsweredAt());
            matchRoundRepository.save(round);
        }
    }
}
