package br.edu.biblioteca.exception;

import br.edu.biblioteca.domain.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;

/**
 * Traduz as exceções da aplicação em respostas HTTP coerentes:
 * 400 dados inválidos · 404 recurso inexistente · 409 operação não permitida pelas regras.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException erro) {
        return resposta(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException erro) {
        return resposta(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException erro) {
        return resposta(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException erro) {
        var campos = new LinkedHashMap<String, String>();
        erro.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        var corpo = new ErroResponse(400, "Dados inválidos.", campos, Instant.now());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido() {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo JSON ausente ou inválido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException erro) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro " + erro.getName() + ".");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade() {
        return resposta(HttpStatus.CONFLICT, "A operação viola uma restrição do banco de dados.");
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), mensagem));
    }
}
