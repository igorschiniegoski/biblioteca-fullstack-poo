package br.edu.biblioteca.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cópia física de um {@link Livro}. Controla a própria situação
 * (DISPONIVEL → EMPRESTADO → DISPONIVEL, ou BAIXADO) e recusa transições inválidas.
 */
@Entity
@Table(name = "exemplar")
public class Exemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoExemplar situacao;

    protected Exemplar() {
        // exigido pelo JPA
    }

    /** Criado apenas por {@link Livro#adicionarExemplar(String)}. */
    Exemplar(Livro livro, String codigo) {
        this.livro = livro;
        this.codigo = Validacao.textoObrigatorio(codigo, "Código do exemplar", 30).toUpperCase();
        this.situacao = SituacaoExemplar.DISPONIVEL;
    }

    public boolean estaDisponivel() {
        return situacao == SituacaoExemplar.DISPONIVEL;
    }

    void emprestar() {
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("O exemplar " + codigo + " foi baixado do acervo.");
        }
        if (situacao == SituacaoExemplar.EMPRESTADO) {
            throw new RegraNegocioException("O exemplar " + codigo + " já está emprestado.");
        }
        situacao = SituacaoExemplar.EMPRESTADO;
    }

    void devolver() {
        if (situacao != SituacaoExemplar.EMPRESTADO) {
            throw new IllegalStateException("O exemplar " + codigo + " não está emprestado.");
        }
        situacao = SituacaoExemplar.DISPONIVEL;
    }

    /** Retira o exemplar de circulação (perda, dano). Não é possível com o exemplar emprestado. */
    public void darBaixa() {
        if (situacao == SituacaoExemplar.EMPRESTADO) {
            throw new RegraNegocioException("Não é possível dar baixa no exemplar " + codigo + " enquanto ele está emprestado.");
        }
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("O exemplar " + codigo + " já foi baixado.");
        }
        situacao = SituacaoExemplar.BAIXADO;
    }

    public Long getId() {
        return id;
    }

    public Livro getLivro() {
        return livro;
    }

    public String getCodigo() {
        return codigo;
    }

    public SituacaoExemplar getSituacao() {
        return situacao;
    }
}
