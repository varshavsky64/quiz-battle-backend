package com.github.varshavsky64.quizbattle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Matchmaking matchmaking = new Matchmaking();
    private Bot bot = new Bot();

    @Data
    public static class Matchmaking {
        private int queueTimeoutSeconds = 10;
        private int roundsPerMatch = 5;
        private int roundTimeoutSeconds = 10;
    }

    @Data
    public static class Bot {
        private double correctAnswerProbability = 0.5;
        private int minAnswerDelaySeconds = 2;
        private int maxAnswerDelaySeconds = 9;
    }
}
