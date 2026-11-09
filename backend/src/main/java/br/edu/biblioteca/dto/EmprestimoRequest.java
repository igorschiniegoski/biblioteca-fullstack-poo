package br.edu.biblioteca.dto;

import jakarta.validation.constraints.NotNull;

public record EmprestimoRequest(
        @NotNull(message = "Informe o leitor.")
        Long leitorId,

        @NotNull(message = "Informe o exemplar.")
        Long exemplarId
) {
}
