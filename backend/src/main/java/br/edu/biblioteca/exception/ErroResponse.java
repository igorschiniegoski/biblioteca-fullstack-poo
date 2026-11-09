package br.edu.biblioteca.exception;

import java.time.Instant;
import java.util.Map;

/** Corpo padrão das respostas de erro da API. */
public record ErroResponse(int status, String mensagem, Map<String, String> campos, Instant momento) {

    public ErroResponse(int status, String mensagem) {
        this(status, mensagem, Map.of(), Instant.now());
    }
}
