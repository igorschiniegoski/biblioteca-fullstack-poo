package br.edu.biblioteca.dto;

import br.edu.biblioteca.domain.Livro;

import java.util.List;

public record LivroResponse(
        Long id,
        String isbn,
        String titulo,
        String autor,
        Integer anoPublicacao,
        int totalExemplares,
        long exemplaresDisponiveis,
        List<ExemplarResponse> exemplares
) {
    public static LivroResponse de(Livro livro) {
        var exemplares = livro.getExemplares().stream().map(ExemplarResponse::de).toList();
        return new LivroResponse(livro.getId(), livro.getIsbn(), livro.getTitulo(), livro.getAutor(),
                livro.getAnoPublicacao(), exemplares.size(), livro.quantidadeDisponivel(), exemplares);
    }
}
