package br.edu.biblioteca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmprestimoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    private Exemplar exemplar;
    private Emprestimo emprestimo;

    @BeforeEach
    void preparar() {
        var leitor = new Leitor("Bruno Lima", "bruno@email.com", null, HOJE);
        var livro = new Livro("9788576082675", "Código Limpo", "Robert C. Martin", 2008);
        exemplar = livro.adicionarExemplar("CL-001");
        emprestimo = leitor.emprestar(exemplar, HOJE);
    }

    @Test
    void emprestimoDefinePrazoDeQuatorzeDiasEOcupaOExemplar() {
        assertEquals(HOJE.plusDays(14), emprestimo.getDataPrevistaDevolucao());
        assertEquals(SituacaoExemplar.EMPRESTADO, exemplar.getSituacao());
        assertTrue(emprestimo.estaAtivo());
    }

    @Test
    void devolucaoNoPrazoNaoGeraMultaELiberaOExemplar() {
        emprestimo.devolver(HOJE.plusDays(14));

        assertEquals(SituacaoEmprestimo.DEVOLVIDO, emprestimo.getSituacao());
        assertEquals(0, emprestimo.getMulta().signum());
        assertTrue(emprestimo.isMultaPaga());
        assertTrue(exemplar.estaDisponivel());
    }

    @Test
    void devolucaoAtrasadaCobraDoisReaisPorDia() {
        emprestimo.devolver(HOJE.plusDays(14 + 5));

        assertEquals(new BigDecimal("10.00"), emprestimo.getMulta());
        assertFalse(emprestimo.isMultaPaga());
        assertEquals(5, emprestimo.diasAtraso(HOJE.plusDays(100)), "atraso fica congelado na data da devolução");
    }

    @Test
    void naoPermiteDevolverDuasVezes() {
        emprestimo.devolver(HOJE.plusDays(3));

        assertThrows(RegraNegocioException.class, () -> emprestimo.devolver(HOJE.plusDays(4)));
    }

    @Test
    void renovacaoEstendePrazoUmaUnicaVez() {
        emprestimo.renovar(HOJE.plusDays(10));

        assertEquals(HOJE.plusDays(28), emprestimo.getDataPrevistaDevolucao());
        assertEquals(1, emprestimo.getRenovacoes());
        var erro = assertThrows(RegraNegocioException.class, () -> emprestimo.renovar(HOJE.plusDays(20)));
        assertTrue(erro.getMessage().contains("limite"));
    }

    @Test
    void naoRenovaEmprestimoAtrasado() {
        var aposPrazo = HOJE.plusDays(15);

        assertTrue(emprestimo.estaAtrasado(aposPrazo));
        assertThrows(RegraNegocioException.class, () -> emprestimo.renovar(aposPrazo));
    }

    @Test
    void naoRenovaEmprestimoDevolvido() {
        emprestimo.devolver(HOJE.plusDays(2));

        assertThrows(RegraNegocioException.class, () -> emprestimo.renovar(HOJE.plusDays(3)));
    }
}
