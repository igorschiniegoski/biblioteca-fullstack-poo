package br.edu.biblioteca.service;

import br.edu.biblioteca.domain.Livro;
import br.edu.biblioteca.domain.RegraNegocioException;
import br.edu.biblioteca.dto.AtualizarLivroRequest;
import br.edu.biblioteca.dto.CriarLivroRequest;
import br.edu.biblioteca.dto.ExemplarRequest;
import br.edu.biblioteca.dto.ExemplarResponse;
import br.edu.biblioteca.dto.LivroResponse;
import br.edu.biblioteca.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.repository.EmprestimoRepository;
import br.edu.biblioteca.repository.ExemplarRepository;
import br.edu.biblioteca.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LivroService {

    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;

    public LivroService(LivroRepository livroRepository, ExemplarRepository exemplarRepository,
                        EmprestimoRepository emprestimoRepository) {
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    @Transactional(readOnly = true)
    public List<LivroResponse> listar() {
        return livroRepository.findAllByOrderByTituloAsc().stream().map(LivroResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public LivroResponse buscar(Long id) {
        return LivroResponse.de(buscarEntidade(id));
    }

    public LivroResponse cadastrar(CriarLivroRequest dados) {
        var livro = new Livro(dados.isbn(), dados.titulo(), dados.autor(), dados.anoPublicacao());
        if (livroRepository.existsByIsbn(livro.getIsbn())) {
            throw new RegraNegocioException("Já existe um livro com o ISBN " + livro.getIsbn() + ".");
        }
        return LivroResponse.de(livroRepository.save(livro));
    }

    public LivroResponse atualizar(Long id, AtualizarLivroRequest dados) {
        var livro = buscarEntidade(id);
        livro.atualizarDados(dados.titulo(), dados.autor(), dados.anoPublicacao());
        return LivroResponse.de(livro);
    }

    /** Livro cujos exemplares já foram emprestados não pode ser excluído (preserva o histórico). */
    public void excluir(Long id) {
        var livro = buscarEntidade(id);
        if (emprestimoRepository.existsByExemplarLivroId(id)) {
            throw new RegraNegocioException(
                    "O livro possui histórico de empréstimos e não pode ser excluído. Dê baixa nos exemplares.");
        }
        livroRepository.delete(livro);
    }

    public ExemplarResponse adicionarExemplar(Long livroId, ExemplarRequest dados) {
        var livro = buscarEntidade(livroId);
        if (exemplarRepository.existsByCodigoIgnoreCase(dados.codigo().strip())) {
            throw new RegraNegocioException("Já existe um exemplar com o código " + dados.codigo().strip() + ".");
        }
        var exemplar = livro.adicionarExemplar(dados.codigo());
        livroRepository.flush();
        return ExemplarResponse.de(exemplar);
    }

    public ExemplarResponse darBaixaExemplar(Long exemplarId) {
        var exemplar = exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar", exemplarId));
        exemplar.darBaixa();
        return ExemplarResponse.de(exemplar);
    }

    private Livro buscarEntidade(Long id) {
        return livroRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Livro", id));
    }
}
