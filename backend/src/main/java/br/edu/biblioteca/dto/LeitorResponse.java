package br.edu.biblioteca.dto;

import br.edu.biblioteca.domain.Leitor;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeitorResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDate dataCadastro,
        long emprestimosAtivos,
        boolean possuiAtraso,
        BigDecimal multasPendentes
) {
    public static LeitorResponse de(Leitor leitor, LocalDate hoje) {
        return new LeitorResponse(
                leitor.getId(), leitor.getNome(), leitor.getEmail(), leitor.getTelefone(),
                leitor.getDataCadastro(), leitor.quantidadeEmprestimosAtivos(),
                leitor.possuiEmprestimoAtrasado(hoje), leitor.totalMultasPendentes());
    }
}
