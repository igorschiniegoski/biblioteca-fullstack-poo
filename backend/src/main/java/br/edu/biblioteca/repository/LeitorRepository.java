package br.edu.biblioteca.repository;

import br.edu.biblioteca.domain.Leitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeitorRepository extends JpaRepository<Leitor, Long> {

    List<Leitor> findAllByOrderByNomeAsc();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
