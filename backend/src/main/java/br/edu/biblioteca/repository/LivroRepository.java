package br.edu.biblioteca.repository;

import br.edu.biblioteca.domain.Livro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    List<Livro> findAllByOrderByTituloAsc();

    boolean existsByIsbn(String isbn);
}
