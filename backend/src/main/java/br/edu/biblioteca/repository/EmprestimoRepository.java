package br.edu.biblioteca.repository;

import br.edu.biblioteca.domain.Emprestimo;
import br.edu.biblioteca.domain.SituacaoEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findAllByOrderByIdDesc();

    List<Emprestimo> findBySituacaoOrderByDataPrevistaDevolucaoAsc(SituacaoEmprestimo situacao);

    List<Emprestimo> findByLeitorIdOrderByIdDesc(Long leitorId);

    /** Consulta derivada que navega pelo relacionamento emprestimo → exemplar → livro. */
    boolean existsByExemplarLivroId(Long livroId);
}
