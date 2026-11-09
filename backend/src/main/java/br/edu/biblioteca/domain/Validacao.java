package br.edu.biblioteca.domain;

/** Validações simples reutilizadas pelas entidades para proteger suas invariantes. */
final class Validacao {

    private Validacao() {
    }

    static String textoObrigatorio(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        var limpo = valor.strip();
        if (limpo.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(campo + " deve ter no máximo " + tamanhoMaximo + " caracteres.");
        }
        return limpo;
    }

    static String textoOpcional(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return textoObrigatorio(valor, campo, tamanhoMaximo);
    }
}
