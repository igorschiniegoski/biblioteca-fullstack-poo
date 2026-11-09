package br.edu.biblioteca.controller;

import br.edu.biblioteca.domain.SituacaoEmprestimo;
import br.edu.biblioteca.dto.EmprestimoRequest;
import br.edu.biblioteca.dto.EmprestimoResponse;
import br.edu.biblioteca.service.EmprestimoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/emprestimos")
public class EmprestimoController {

    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmprestimoResponse> listar(@RequestParam(required = false) SituacaoEmprestimo situacao) {
        return service.listar(situacao);
    }

    @GetMapping("/{id}")
    public EmprestimoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<EmprestimoResponse> emprestar(@Valid @RequestBody EmprestimoRequest dados) {
        var criado = service.emprestar(dados);
        return ResponseEntity.created(URI.create("/api/emprestimos/" + criado.id())).body(criado);
    }

    @PatchMapping("/{id}/devolucao")
    public EmprestimoResponse devolver(@PathVariable Long id) {
        return service.devolver(id);
    }

    @PatchMapping("/{id}/renovacao")
    public EmprestimoResponse renovar(@PathVariable Long id) {
        return service.renovar(id);
    }
}
