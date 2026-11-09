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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Empréstimo de um {@link Exemplar} para um {@link Leitor}.
 * Concentra as regras de prazo, renovação, devolução e multa por atraso.
 */
@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    public static final int PRAZO_DIAS = 14;
    public static final int LIMITE_RENOVACOES = 1;
    public static final BigDecimal MULTA_POR_DIA = new BigDecimal("2.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leitor_id", nullable = false)
    private Leitor leitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id", nullable = false)
    private Exemplar exemplar;

    @Column(name = "data_emprestimo", nullable = false)
    private LocalDate dataEmprestimo;

    @Column(name = "data_prevista_devolucao", nullable = false)
    private LocalDate dataPrevistaDevolucao;

    @Column(name = "data_devolucao")
    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private int renovacoes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoEmprestimo situacao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal multa = BigDecimal.ZERO.setScale(2);

    @Column(name = "multa_paga", nullable = false)
    private boolean multaPaga = true;

    protected Emprestimo() {
        // exigido pelo JPA
    }

    /** Criado apenas por {@link Leitor#emprestar(Exemplar, LocalDate)}, que aplica as regras do leitor. */
    Emprestimo(Leitor leitor, Exemplar exemplar, LocalDate hoje) {
        this.leitor = Objects.requireNonNull(leitor);
        this.exemplar = Objects.requireNonNull(exemplar);
        this.dataEmprestimo = Objects.requireNonNull(hoje, "Data do empréstimo é obrigatória.");
        exemplar.emprestar();
        this.dataPrevistaDevolucao = hoje.plusDays(PRAZO_DIAS);
        this.situacao = SituacaoEmprestimo.ATIVO;
    }

    /** Devolve o exemplar, libera-o para novos empréstimos e calcula a multa por atraso. */
    public void devolver(LocalDate hoje) {
        if (situacao != SituacaoEmprestimo.ATIVO) {
            throw new RegraNegocioException("Este empréstimo já foi devolvido.");
        }
        if (hoje.isBefore(dataEmprestimo)) {
            throw new IllegalArgumentException("A data de devolução não pode ser anterior à data do empréstimo.");
        }
        this.dataDevolucao = hoje;
        this.multa = MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso(hoje)));
        this.multaPaga = multa.signum() == 0;
        this.situacao = SituacaoEmprestimo.DEVOLVIDO;
        exemplar.devolver();
    }

    /** Estende o prazo em {@value #PRAZO_DIAS} dias. Só é permitido uma vez e se não estiver atrasado. */
    public void renovar(LocalDate hoje) {
        if (situacao != SituacaoEmprestimo.ATIVO) {
            throw new RegraNegocioException("Só é possível renovar um empréstimo ativo.");
        }
        if (estaAtrasado(hoje)) {
            throw new RegraNegocioException("Empréstimo em atraso não pode ser renovado.");
        }
        if (renovacoes >= LIMITE_RENOVACOES) {
            throw new RegraNegocioException("O empréstimo já atingiu o limite de " + LIMITE_RENOVACOES + " renovação.");
        }
        this.dataPrevistaDevolucao = dataPrevistaDevolucao.plusDays(PRAZO_DIAS);
        this.renovacoes++;
    }

    public long diasAtraso(LocalDate hoje) {
        var referencia = dataDevolucao != null ? dataDevolucao : hoje;
        return Math.max(0, ChronoUnit.DAYS.between(dataPrevistaDevolucao, referencia));
    }

    public boolean estaAtivo() {
        return situacao == SituacaoEmprestimo.ATIVO;
    }

    public boolean estaAtrasado(LocalDate hoje) {
        return estaAtivo() && hoje.isAfter(dataPrevistaDevolucao);
    }

    boolean possuiMultaPendente() {
        return multa.signum() > 0 && !multaPaga;
    }

    void quitarMulta() {
        this.multaPaga = true;
    }

    public Long getId() {
        return id;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public LocalDate getDataPrevistaDevolucao() {
        return dataPrevistaDevolucao;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public int getRenovacoes() {
        return renovacoes;
    }

    public SituacaoEmprestimo getSituacao() {
        return situacao;
    }

    public BigDecimal getMulta() {
        return multa;
    }

    public boolean isMultaPaga() {
        return multaPaga;
    }
}
