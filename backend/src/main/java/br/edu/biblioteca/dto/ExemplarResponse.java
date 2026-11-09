package br.edu.biblioteca.dto;

import br.edu.biblioteca.domain.Exemplar;
import br.edu.biblioteca.domain.SituacaoExemplar;

public record ExemplarResponse(Long id, String codigo, SituacaoExemplar situacao, Long livroId, String tituloLivro) {

    public static ExemplarResponse de(Exemplar exemplar) {
        return new ExemplarResponse(exemplar.getId(), exemplar.getCodigo(), exemplar.getSituacao(),
                exemplar.getLivro().getId(), exemplar.getLivro().getTitulo());
    }
}
