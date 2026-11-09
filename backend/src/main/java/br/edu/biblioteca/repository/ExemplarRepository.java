package br.edu.biblioteca.repository;

import br.edu.biblioteca.domain.Exemplar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);
}
