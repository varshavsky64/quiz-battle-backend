package com.github.varshavsky64.quizbattle.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionRequest {
    @NotBlank
    private String text;

    private short difficulty = 1;

    @NotEmpty
    @Size(min = 2, max = 4)
    private List<AnswerRequest> answers;

    @Getter
    @Setter
    public static class AnswerRequest {
        @NotBlank
        private String text;
        private boolean correct;
        private short position;
    }
}
