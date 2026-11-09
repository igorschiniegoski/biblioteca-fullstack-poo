package br.edu.biblioteca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    private Leitor leitor;
    private Livro livro;

    @BeforeEach
    void preparar() {
        leitor = new Leitor("Ana Souza", "ana@email.com", null, HOJE);
        livro = new Livro("9788535902778", "Dom Casmurro", "Machado de Assis", 1899);
    }

    @Test
    void naoPermiteMaisDeTresEmprestimosAtivos() {
        // Arrange
        for (int i = 1; i <= Leitor.LIMITE_EMPRESTIMOS_ATIVOS; i++) {
            leitor.emprestar(livro.adicionarExemplar("EX-" + i), HOJE);
        }
        var quarto = livro.adicionarExemplar("EX-4");

        // Act
        var erro = assertThrows(RegraNegocioException.class, () -> leitor.emprestar(quarto, HOJE));

        // Assert
        assertTrue(erro.getMessage().contains("limite"));
        assertTrue(quarto.estaDisponivel(), "o exemplar recusado deve continuar disponível");
    }

    @Test
    void devolverUmLivroLiberaNovoEmprestimoDentroDoLimite() {
        Emprestimo primeiro = null;
        for (int i = 1; i <= Leitor.LIMITE_EMPRESTIMOS_ATIVOS; i++) {
            var e = leitor.emprestar(livro.adicionarExemplar("EX-" + i), HOJE);
            if (primeiro == null) primeiro = e;
        }

        primeiro.devolver(HOJE.plusDays(2));
        leitor.emprestar(livro.adicionarExemplar("EX-4"), HOJE.plusDays(2));

        assertEquals(3, leitor.quantidadeEmprestimosAtivos());
    }

    @Test
    void leitorComEmprestimoAtrasadoNaoPodePegarOutroLivro() {
        leitor.emprestar(livro.adicionarExemplar("EX-1"), HOJE);
        var diaAposPrazo = HOJE.plusDays(Emprestimo.PRAZO_DIAS + 1);

        var erro = assertThrows(RegraNegocioException.class,
                () -> leitor.emprestar(livro.adicionarExemplar("EX-2"), diaAposPrazo));

        assertTrue(erro.getMessage().contains("atraso"));
    }

    @Test
    void leitorComMultaPendenteNaoPodePegarOutroLivro() {
        var emprestimo = leitor.emprestar(livro.adicionarExemplar("EX-1"), HOJE);
        var devolucao = HOJE.plusDays(Emprestimo.PRAZO_DIAS + 3);
        emprestimo.devolver(devolucao);

        var erro = assertThrows(RegraNegocioException.class,
                () -> leitor.emprestar(livro.adicionarExemplar("EX-2"), devolucao));

        assertTrue(erro.getMessage().contains("multas pendentes"));
        assertEquals(new BigDecimal("6.00"), leitor.totalMultasPendentes());
    }

    @Test
    void quitarMultasDesbloqueiaOLeitor() {
        var emprestimo = leitor.emprestar(livro.adicionarExemplar("EX-1"), HOJE);
        var devolucao = HOJE.plusDays(Emprestimo.PRAZO_DIAS + 2);
        emprestimo.devolver(devolucao);

        var pago = leitor.quitarMultas();
        leitor.emprestar(livro.adicionarExemplar("EX-2"), devolucao);

        assertEquals(new BigDecimal("4.00"), pago);
        assertEquals(BigDecimal.ZERO, leitor.totalMultasPendentes());
        assertEquals(1, leitor.quantidadeEmprestimosAtivos());
    }

    @Test
    void naoPermiteQuitarQuandoNaoHaMultas() {
        assertThrows(RegraNegocioException.class, () -> leitor.quitarMultas());
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Leitor(" ", "a@b.com", null, HOJE));
        assertThrows(IllegalArgumentException.class, () -> new Leitor("Ana", "email-invalido", null, HOJE));
        assertEquals("ana@email.com", new Leitor("Ana", "  ANA@Email.com ", null, HOJE).getEmail());
    }
}
