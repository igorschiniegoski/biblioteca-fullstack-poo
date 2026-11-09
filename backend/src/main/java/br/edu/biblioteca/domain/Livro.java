package br.edu.biblioteca.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Obra do acervo (título, autor, ISBN). Um livro é composto por vários
 * {@link Exemplar exemplares} físicos: os exemplares só existem por meio do livro
 * (composição) e são criados pelo método {@link #adicionarExemplar(String)}.
 */
@Entity
@Table(name = "livro")
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 150)
    private String autor;

    @Column(name = "ano_publicacao")
    private Integer anoPublicacao;

    @OneToMany(mappedBy = "livro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("codigo")
    private List<Exemplar> exemplares = new ArrayList<>();

    protected Livro() {
        // exigido pelo JPA
    }

    public Livro(String isbn, String titulo, String autor, Integer anoPublicacao) {
        this.isbn = normalizarIsbn(isbn);
        atualizarDados(titulo, autor, anoPublicacao);
    }

    /** O ISBN identifica a obra e não muda; título, autor e ano podem ser corrigidos. */
    public void atualizarDados(String titulo, String autor, Integer anoPublicacao) {
        this.titulo = Validacao.textoObrigatorio(titulo, "Título", 200);
        this.autor = Validacao.textoObrigatorio(autor, "Autor", 150);
        if (anoPublicacao != null && (anoPublicacao < 1450 || anoPublicacao > Year.now().getValue())) {
            throw new IllegalArgumentException("Ano de publicação inválido.");
        }
        this.anoPublicacao = anoPublicacao;
    }

    public Exemplar adicionarExemplar(String codigo) {
        var exemplar = new Exemplar(this, codigo);
        boolean repetido = exemplares.stream().anyMatch(e -> e.getCodigo().equalsIgnoreCase(exemplar.getCodigo()));
        if (repetido) {
            throw new RegraNegocioException("Já existe um exemplar com o código " + exemplar.getCodigo() + ".");
        }
        exemplares.add(exemplar);
        return exemplar;
    }

    public long quantidadeDisponivel() {
        return exemplares.stream().filter(Exemplar::estaDisponivel).count();
    }

    /** Aceita ISBN-10 ou ISBN-13, com ou sem hífens, e guarda apenas os dígitos. */
    static String normalizarIsbn(String isbn) {
        var digitos = Validacao.textoObrigatorio(isbn, "ISBN", 17).replace("-", "").replace(" ", "");
        boolean valido = digitos.matches("\\d{13}") || digitos.matches("\\d{9}[\\dXx]");
        if (!valido) {
            throw new IllegalArgumentException("ISBN deve ter 10 ou 13 dígitos.");
        }
        return digitos.toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public Integer getAnoPublicacao() {
        return anoPublicacao;
    }

    public List<Exemplar> getExemplares() {
        return Collections.unmodifiableList(exemplares);
    }
}
