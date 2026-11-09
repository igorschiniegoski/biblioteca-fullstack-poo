package br.edu.biblioteca.domain;

/**
 * Lançada quando uma operação viola uma regra de negócio do domínio
 * (ex.: emprestar um exemplar que já está emprestado).
 * A API traduz essa exceção para HTTP 409 Conflict.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
