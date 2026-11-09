package br.edu.biblioteca.controller;

import br.edu.biblioteca.dto.AtualizarLivroRequest;
import br.edu.biblioteca.dto.CriarLivroRequest;
import br.edu.biblioteca.dto.ExemplarRequest;
import br.edu.biblioteca.dto.ExemplarResponse;
import br.edu.biblioteca.dto.LivroResponse;
import br.edu.biblioteca.service.LivroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class LivroController {

    private final LivroService service;

    public LivroController(LivroService service) {
        this.service = service;
    }

    @GetMapping("/api/livros")
    public List<LivroResponse> listar() {
        return service.listar();
    }

    @GetMapping("/api/livros/{id}")
    public LivroResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/api/livros")
    public ResponseEntity<LivroResponse> cadastrar(@Valid @RequestBody CriarLivroRequest dados) {
        var criado = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/livros/" + criado.id())).body(criado);
    }

    @PutMapping("/api/livros/{id}")
    public LivroResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarLivroRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/api/livros/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/livros/{id}/exemplares")
    public ResponseEntity<ExemplarResponse> adicionarExemplar(@PathVariable Long id,
                                                              @Valid @RequestBody ExemplarRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarExemplar(id, dados));
    }

    @PatchMapping("/api/exemplares/{id}/baixa")
    public ExemplarResponse darBaixa(@PathVariable Long id) {
        return service.darBaixaExemplar(id);
    }
}
