package br.edu.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExemplarRequest(
        @NotBlank(message = "Código do exemplar é obrigatório.")
        @Size(max = 30, message = "Código deve ter no máximo 30 caracteres.")
        String codigo
) {
}
