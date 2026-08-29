package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "O E-mail nao deve ser vazio")
        String email
) {}