package com.github.varshavsky64.quizbattle.domain.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnswerOption {
    private String id;
    private String text;
    private int position;
}
