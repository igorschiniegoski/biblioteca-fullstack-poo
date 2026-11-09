package br.edu.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O ISBN não entra na atualização: ele identifica a obra e é imutável. */
public record AtualizarLivroRequest(
        @NotBlank(message = "Título é obrigatório.")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres.")
        String titulo,

        @NotBlank(message = "Autor é obrigatório.")
        @Size(max = 150, message = "Autor deve ter no máximo 150 caracteres.")
        String autor,

        Integer anoPublicacao
) {
}
