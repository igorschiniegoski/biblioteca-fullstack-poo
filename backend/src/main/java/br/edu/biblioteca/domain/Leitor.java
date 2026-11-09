package br.edu.biblioteca.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pessoa cadastrada que pode pegar livros emprestados.
 * É o leitor quem decide se pode ou não fazer um novo empréstimo
 * ({@link #emprestar(Exemplar, LocalDate)}), aplicando as regras da biblioteca.
 */
@Entity
@Table(name = "leitor")
public class Leitor {

    public static final int LIMITE_EMPRESTIMOS_ATIVOS = 3;
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @OneToMany(mappedBy = "leitor")
    private List<Emprestimo> emprestimos = new ArrayList<>();

    protected Leitor() {
        // exigido pelo JPA
    }

    public Leitor(String nome, String email, String telefone, LocalDate dataCadastro) {
        atualizarDados(nome, email, telefone);
        this.dataCadastro = Objects.requireNonNull(dataCadastro, "Data de cadastro é obrigatória.");
    }

    public void atualizarDados(String nome, String email, String telefone) {
        this.nome = Validacao.textoObrigatorio(nome, "Nome", 100);
        var emailLimpo = Validacao.textoObrigatorio(email, "E-mail", 150).toLowerCase();
        if (!EMAIL.matcher(emailLimpo).matches()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        this.email = emailLimpo;
        this.telefone = Validacao.textoOpcional(telefone, "Telefone", 20);
    }

    /**
     * Regras de negócio do empréstimo:
     * 1. no máximo {@value #LIMITE_EMPRESTIMOS_ATIVOS} empréstimos ativos ao mesmo tempo;
     * 2. leitor com empréstimo em atraso não pode pegar outro livro;
     * 3. leitor com multa pendente não pode pegar outro livro;
     * 4. o exemplar precisa estar disponível (verificado pelo próprio exemplar).
     */
    public Emprestimo emprestar(Exemplar exemplar, LocalDate hoje) {
        Objects.requireNonNull(exemplar, "Exemplar é obrigatório.");
        if (quantidadeEmprestimosAtivos() >= LIMITE_EMPRESTIMOS_ATIVOS) {
            throw new RegraNegocioException(
                    "O leitor já possui " + LIMITE_EMPRESTIMOS_ATIVOS + " empréstimos ativos, que é o limite permitido.");
        }
        if (possuiEmprestimoAtrasado(hoje)) {
            throw new RegraNegocioException("O leitor possui empréstimo em atraso e não pode pegar outro livro.");
        }
        var multas = totalMultasPendentes();
        if (multas.signum() > 0) {
            throw new RegraNegocioException(
                    "O leitor possui multas pendentes de R$ " + multas.toPlainString().replace('.', ',') + ".");
        }
        var emprestimo = new Emprestimo(this, exemplar, hoje);
        emprestimos.add(emprestimo);
        return emprestimo;
    }

    /** Quita todas as multas pendentes e devolve o valor pago. */
    public BigDecimal quitarMultas() {
        var total = totalMultasPendentes();
        if (total.signum() == 0) {
            throw new RegraNegocioException("O leitor não possui multas pendentes.");
        }
        emprestimos.stream().filter(Emprestimo::possuiMultaPendente).forEach(Emprestimo::quitarMulta);
        return total;
    }

    public long quantidadeEmprestimosAtivos() {
        return emprestimos.stream().filter(Emprestimo::estaAtivo).count();
    }

    public boolean possuiEmprestimoAtrasado(LocalDate hoje) {
        return emprestimos.stream().anyMatch(e -> e.estaAtrasado(hoje));
    }

    public BigDecimal totalMultasPendentes() {
        return emprestimos.stream()
                .filter(Emprestimo::possuiMultaPendente)
                .map(Emprestimo::getMulta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Um leitor com histórico de empréstimos não pode ser excluído, para não perder o histórico. */
    public boolean possuiHistorico() {
        return !emprestimos.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public List<Emprestimo> getEmprestimos() {
        return Collections.unmodifiableList(emprestimos);
    }
}
