package br.edu.biblioteca.service;

import br.edu.biblioteca.domain.Leitor;
import br.edu.biblioteca.domain.RegraNegocioException;
import br.edu.biblioteca.dto.EmprestimoResponse;
import br.edu.biblioteca.dto.LeitorRequest;
import br.edu.biblioteca.dto.LeitorResponse;
import br.edu.biblioteca.dto.PagamentoMultaResponse;
import br.edu.biblioteca.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.repository.EmprestimoRepository;
import br.edu.biblioteca.repository.LeitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LeitorService {

    private final LeitorRepository leitorRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final Clock clock;

    public LeitorService(LeitorRepository leitorRepository, EmprestimoRepository emprestimoRepository, Clock clock) {
        this.leitorRepository = leitorRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LeitorResponse> listar() {
        var hoje = hoje();
        return leitorRepository.findAllByOrderByNomeAsc().stream().map(l -> LeitorResponse.de(l, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public LeitorResponse buscar(Long id) {
        return LeitorResponse.de(buscarEntidade(id), hoje());
    }

    public LeitorResponse cadastrar(LeitorRequest dados) {
        if (leitorRepository.existsByEmailIgnoreCase(dados.email().strip())) {
            throw new RegraNegocioException("Já existe um leitor com o e-mail " + dados.email() + ".");
        }
        var leitor = new Leitor(dados.nome(), dados.email(), dados.telefone(), hoje());
        return LeitorResponse.de(leitorRepository.save(leitor), hoje());
    }

    public LeitorResponse atualizar(Long id, LeitorRequest dados) {
        var leitor = buscarEntidade(id);
        if (leitorRepository.existsByEmailIgnoreCaseAndIdNot(dados.email().strip(), id)) {
            throw new RegraNegocioException("Já existe outro leitor com o e-mail " + dados.email() + ".");
        }
        leitor.atualizarDados(dados.nome(), dados.email(), dados.telefone());
        return LeitorResponse.de(leitor, hoje());
    }

    public void excluir(Long id) {
        var leitor = buscarEntidade(id);
        if (leitor.possuiHistorico()) {
            throw new RegraNegocioException("Leitor com histórico de empréstimos não pode ser excluído.");
        }
        leitorRepository.delete(leitor);
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponse> historico(Long id) {
        buscarEntidade(id);
        var hoje = hoje();
        return emprestimoRepository.findByLeitorIdOrderByIdDesc(id).stream()
                .map(e -> EmprestimoResponse.de(e, hoje)).toList();
    }

    public PagamentoMultaResponse pagarMultas(Long id) {
        var leitor = buscarEntidade(id);
        var valor = leitor.quitarMultas();
        return new PagamentoMultaResponse(id, valor);
    }

    Leitor buscarEntidade(Long id) {
        return leitorRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Leitor", id));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
