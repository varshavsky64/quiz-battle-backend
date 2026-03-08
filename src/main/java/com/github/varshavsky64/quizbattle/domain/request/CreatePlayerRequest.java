package com.github.varshavsky64.quizbattle.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class CreatePlayerRequest {
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;
}
