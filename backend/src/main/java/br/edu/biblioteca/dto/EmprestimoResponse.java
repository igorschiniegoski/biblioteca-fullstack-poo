package br.edu.biblioteca.dto;

import br.edu.biblioteca.domain.Emprestimo;
import br.edu.biblioteca.domain.SituacaoEmprestimo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmprestimoResponse(
        Long id,
        Long leitorId,
        String leitorNome,
        Long exemplarId,
        String exemplarCodigo,
        String livroTitulo,
        LocalDate dataEmprestimo,
        LocalDate dataPrevistaDevolucao,
        LocalDate dataDevolucao,
        int renovacoes,
        SituacaoEmprestimo situacao,
        boolean atrasado,
        long diasAtraso,
        BigDecimal multa,
        boolean multaPaga
) {
    public static EmprestimoResponse de(Emprestimo e, LocalDate hoje) {
        var exemplar = e.getExemplar();
        return new EmprestimoResponse(
                e.getId(), e.getLeitor().getId(), e.getLeitor().getNome(),
                exemplar.getId(), exemplar.getCodigo(), exemplar.getLivro().getTitulo(),
                e.getDataEmprestimo(), e.getDataPrevistaDevolucao(), e.getDataDevolucao(),
                e.getRenovacoes(), e.getSituacao(), e.estaAtrasado(hoje), e.diasAtraso(hoje),
                e.getMulta(), e.isMultaPaga());
    }
}
