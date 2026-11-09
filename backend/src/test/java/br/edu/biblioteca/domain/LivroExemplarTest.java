package br.edu.biblioteca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LivroExemplarTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    private Livro livro;
    private Leitor leitor;

    @BeforeEach
    void preparar() {
        livro = new Livro("978-85-7522-418-2", "Padrões de Projeto", "Erich Gamma", 1994);
        leitor = new Leitor("Carla", "carla@email.com", null, HOJE);
    }

    @Test
    void naoEmprestaExemplarQueJaEstaEmprestado() {
        var exemplar = livro.adicionarExemplar("GOF-001");
        leitor.emprestar(exemplar, HOJE);
        var outroLeitor = new Leitor("Davi", "davi@email.com", null, HOJE);

        var erro = assertThrows(RegraNegocioException.class, () -> outroLeitor.emprestar(exemplar, HOJE));

        assertEquals("O exemplar GOF-001 já está emprestado.", erro.getMessage());
        assertEquals(0, outroLeitor.quantidadeEmprestimosAtivos());
    }

    @Test
    void naoDaBaixaEmExemplarEmprestado() {
        var exemplar = livro.adicionarExemplar("GOF-001");
        leitor.emprestar(exemplar, HOJE);

        assertThrows(RegraNegocioException.class, exemplar::darBaixa);
        assertEquals(SituacaoExemplar.EMPRESTADO, exemplar.getSituacao());
    }

    @Test
    void exemplarBaixadoNaoPodeSerEmprestado() {
        var exemplar = livro.adicionarExemplar("GOF-001");
        exemplar.darBaixa();

        assertThrows(RegraNegocioException.class, () -> leitor.emprestar(exemplar, HOJE));
        assertThrows(RegraNegocioException.class, exemplar::darBaixa);
    }

    @Test
    void contaApenasExemplaresDisponiveis() {
        leitor.emprestar(livro.adicionarExemplar("GOF-001"), HOJE);
        livro.adicionarExemplar("GOF-002").darBaixa();
        livro.adicionarExemplar("GOF-003");

        assertEquals(1, livro.quantidadeDisponivel());
    }

    @Test
    void naoAceitaCodigoDeExemplarRepetidoNoMesmoLivro() {
        livro.adicionarExemplar("gof-001");

        assertThrows(RegraNegocioException.class, () -> livro.adicionarExemplar("GOF-001"));
    }

    @Test
    void normalizaEValidaIsbn() {
        assertEquals("9788575224182", livro.getIsbn());
        assertThrows(IllegalArgumentException.class, () -> new Livro("123", "Título", "Autor", null));
        assertThrows(IllegalArgumentException.class, () -> new Livro("9788575224182", "Título", "Autor", 1200));
    }
}
