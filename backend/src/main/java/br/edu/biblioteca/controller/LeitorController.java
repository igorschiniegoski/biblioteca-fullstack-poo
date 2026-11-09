package br.edu.biblioteca.controller;

import br.edu.biblioteca.dto.EmprestimoResponse;
import br.edu.biblioteca.dto.LeitorRequest;
import br.edu.biblioteca.dto.LeitorResponse;
import br.edu.biblioteca.dto.PagamentoMultaResponse;
import br.edu.biblioteca.service.LeitorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/leitores")
public class LeitorController {

    private final LeitorService service;

    public LeitorController(LeitorService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeitorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public LeitorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<LeitorResponse> cadastrar(@Valid @RequestBody LeitorRequest dados) {
        var criado = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/leitores/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public LeitorResponse atualizar(@PathVariable Long id, @Valid @RequestBody LeitorRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/emprestimos")
    public List<EmprestimoResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    @PostMapping("/{id}/pagamento-multas")
    public PagamentoMultaResponse pagarMultas(@PathVariable Long id) {
        return service.pagarMultas(id);
    }
}
