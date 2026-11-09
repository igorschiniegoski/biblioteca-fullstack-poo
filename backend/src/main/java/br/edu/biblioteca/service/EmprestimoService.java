package br.edu.biblioteca.service;

import br.edu.biblioteca.domain.Emprestimo;
import br.edu.biblioteca.domain.SituacaoEmprestimo;
import br.edu.biblioteca.dto.EmprestimoRequest;
import br.edu.biblioteca.dto.EmprestimoResponse;
import br.edu.biblioteca.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.repository.EmprestimoRepository;
import br.edu.biblioteca.repository.ExemplarRepository;
import br.edu.biblioteca.repository.LeitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordena os casos de uso de empréstimo. As regras ficam no domínio:
 * o service apenas busca os objetos, delega a decisão a eles e persiste o resultado.
 */
@Service
@Transactional
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final LeitorRepository leitorRepository;
    private final ExemplarRepository exemplarRepository;
    private final Clock clock;

    public EmprestimoService(EmprestimoRepository emprestimoRepository, LeitorRepository leitorRepository,
                             ExemplarRepository exemplarRepository, Clock clock) {
        this.emprestimoRepository = emprestimoRepository;
        this.leitorRepository = leitorRepository;
        this.exemplarRepository = exemplarRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listar(SituacaoEmprestimo situacao) {
        var hoje = hoje();
        var emprestimos = situacao == null
                ? emprestimoRepository.findAllByOrderByIdDesc()
                : emprestimoRepository.findBySituacaoOrderByDataPrevistaDevolucaoAsc(situacao);
        return emprestimos.stream().map(e -> EmprestimoResponse.de(e, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public EmprestimoResponse buscar(Long id) {
        return EmprestimoResponse.de(buscarEntidade(id), hoje());
    }

    public EmprestimoResponse emprestar(EmprestimoRequest dados) {
        var leitor = leitorRepository.findById(dados.leitorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Leitor", dados.leitorId()));
        var exemplar = exemplarRepository.findById(dados.exemplarId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar", dados.exemplarId()));

        var emprestimo = leitor.emprestar(exemplar, hoje());

        return EmprestimoResponse.de(emprestimoRepository.save(emprestimo), hoje());
    }

    public EmprestimoResponse devolver(Long id) {
        var emprestimo = buscarEntidade(id);
        emprestimo.devolver(hoje());
        return EmprestimoResponse.de(emprestimo, hoje());
    }

    public EmprestimoResponse renovar(Long id) {
        var emprestimo = buscarEntidade(id);
        emprestimo.renovar(hoje());
        return EmprestimoResponse.de(emprestimo, hoje());
    }

    private Emprestimo buscarEntidade(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo", id));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
