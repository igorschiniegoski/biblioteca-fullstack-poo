# 📚 Sistema de Biblioteca — Full Stack com Spring Boot e ReactJS

Atividade Final do 2º bimestre de **Programação Orientada a Objetos** (ADSIS4S – Unicesumar Ponta Grossa).

## Integrantes

- Igor Schiniegoski Pallisser
- Luis Boratto

## Tema e objetivo

Sistema para controlar o **acervo e os empréstimos de uma biblioteca**. Ele permite cadastrar leitores,
livros e seus exemplares físicos, registrar empréstimos, devoluções e renovações, calcular multa por atraso
e impedir operações que violam as regras da biblioteca.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Frontend | ReactJS 19, React Router, Vite |
| Backend | Java 26, Spring Boot 4.1, Spring Web MVC, Bean Validation |
| Persistência | JPA / Spring Data JPA (Hibernate) |
| Banco de dados | **MySQL 8** |
| Testes | JUnit 5, Spring Boot Test, H2 (em memória, só nos testes) |

## Arquitetura

```
ReactJS  (frontend/src/pages, api.js)
   ↓  JSON via HTTP
API REST  (controller/)       rotas, parâmetros, status HTTP
   ↓
Service  (service/)           coordena os casos de uso e as transações
   ↓
Domain / Entity  (domain/)    regras de negócio, invariantes, mudanças de estado
   ↓
Repository  (repository/)     interfaces Spring Data JPA
   ↓
JPA / Hibernate
   ↓
MySQL  (database/schema.sql)
```

```
backend/src/main/java/br/edu/biblioteca/
├── BibliotecaApplication.java
├── controller/   LeitorController, LivroController, EmprestimoController
├── service/      LeitorService, LivroService, EmprestimoService
├── domain/       Leitor, Livro, Exemplar, Emprestimo, SituacaoExemplar, SituacaoEmprestimo, RegraNegocioException
├── repository/   LeitorRepository, LivroRepository, ExemplarRepository, EmprestimoRepository
├── dto/          records de entrada (Request) e saída (Response) da API
├── exception/    RecursoNaoEncontradoException, ErroResponse, GlobalExceptionHandler
└── config/       AppConfig (Clock injetável e CORS)
```

### Onde ficam as regras

As regras ficam **nas entidades**, não no controller nem no service. O service só busca os objetos e pede
que eles executem a operação. Por exemplo, em `EmprestimoService.emprestar`:

```java
var leitor = leitorRepository.findById(...)
var exemplar = exemplarRepository.findById(...)
var emprestimo = leitor.emprestar(exemplar, hoje());   // o Leitor decide se pode
return emprestimoRepository.save(emprestimo);
```

As entidades não têm setters públicos. O estado só muda por métodos com significado de negócio
(`emprestar`, `devolver`, `renovar`, `darBaixa`, `quitarMultas`), que validam as invariantes antes de mudar.
Os construtores de `Exemplar` e `Emprestimo` são de pacote: um exemplar só nasce por `livro.adicionarExemplar(...)`
(composição) e um empréstimo só nasce por `leitor.emprestar(...)`.

## Regras de negócio (além do CRUD)

1. **Limite de 3 empréstimos ativos** por leitor.
2. **Exemplar indisponível não pode ser emprestado**: emprestado ou baixado.
3. **Leitor com empréstimo em atraso fica bloqueado** para novos empréstimos.
4. **Multa de R$ 2,00 por dia de atraso**, calculada na devolução. Leitor com multa pendente fica bloqueado até quitar.
5. **Prazo de 14 dias**, com **no máximo 1 renovação**, e só se o empréstimo não estiver atrasado.
6. **Exemplar emprestado não pode receber baixa.**
7. **Leitor ou livro com histórico de empréstimos não pode ser excluído**, para preservar o histórico.
8. E-mail do leitor, ISBN do livro e código do exemplar são únicos. O ISBN é validado (10 ou 13 dígitos) e não pode ser alterado.

## API REST

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| GET | `/api/leitores` | Lista leitores, com situação (atraso, multas) | 200 |
| GET | `/api/leitores/{id}` | Busca um leitor | 200 |
| POST | `/api/leitores` | Cadastra leitor | 201 |
| PUT | `/api/leitores/{id}` | Atualiza leitor | 200 |
| DELETE | `/api/leitores/{id}` | Exclui leitor sem histórico | 204 |
| GET | `/api/leitores/{id}/emprestimos` | Histórico de empréstimos do leitor | 200 |
| POST | `/api/leitores/{id}/pagamento-multas` | Quita as multas pendentes | 200 |
| GET | `/api/livros` | Lista livros com exemplares e disponibilidade | 200 |
| GET | `/api/livros/{id}` | Busca um livro | 200 |
| POST | `/api/livros` | Cadastra livro | 201 |
| PUT | `/api/livros/{id}` | Atualiza título, autor e ano | 200 |
| DELETE | `/api/livros/{id}` | Exclui livro sem histórico | 204 |
| POST | `/api/livros/{id}/exemplares` | Adiciona exemplar ao livro | 201 |
| PATCH | `/api/exemplares/{id}/baixa` | Dá baixa em um exemplar | 200 |
| GET | `/api/emprestimos?situacao=ATIVO` | Lista empréstimos (filtro opcional `ATIVO`/`DEVOLVIDO`) | 200 |
| GET | `/api/emprestimos/{id}` | Busca um empréstimo | 200 |
| POST | `/api/emprestimos` | Registra empréstimo `{ "leitorId": 1, "exemplarId": 2 }` | 201 |
| PATCH | `/api/emprestimos/{id}/devolucao` | Registra devolução e calcula multa | 200 |
| PATCH | `/api/emprestimos/{id}/renovacao` | Renova por mais 14 dias | 200 |

**Erros** (corpo `{ "status", "mensagem", "campos", "momento" }`, tratado em `GlobalExceptionHandler`):

| Situação | Status |
|---|---|
| Dados inválidos: campo obrigatório vazio, e-mail ou ISBN inválido, JSON malformado | **400** (com `campos` detalhando cada erro) |
| Leitor, livro, exemplar ou empréstimo inexistente | **404** |
| Operação não permitida pelas regras: limite, atraso, multa, exemplar indisponível, duplicidade | **409** |

## Banco de dados

O banco utilizado é o **MySQL 8**. A modelagem completa (diagrama ER, cardinalidades e o mapeamento
objeto → JPA → chave estrangeira) está em [`docs/modelagem.md`](docs/modelagem.md).

- `database/schema.sql`: `CREATE TABLE` com PKs, FKs, `UNIQUE`, `CHECK` e `NOT NULL`.
- `database/dados-iniciais.sql`: dados de exemplo para a demonstração, já com um leitor em atraso e um com multa.

O backend usa `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate **não cria** as tabelas. Na inicialização,
ele confere se as entidades batem com as tabelas criadas pelo `schema.sql` e falha se algo estiver diferente.

### JDBC × JPA

Na atividade anterior, o acesso a dados era feito à mão com JDBC: escrever cada `INSERT`/`SELECT`, abrir a
conexão, preencher o `PreparedStatement` e converter cada linha do `ResultSet` em objeto. Com JPA / Spring Data JPA:

- o **mapeamento objeto-relacional** (`@Entity`, `@Column`, `@ManyToOne`...) substitui a conversão manual de `ResultSet`;
- os **repositórios** são só interfaces. O Spring gera a implementação de `save`, `findById`, `delete` e das
  consultas derivadas do nome do método (ex.: `existsByExemplarLivroId` navega emprestimo → exemplar → livro);
- o Hibernate gera o SQL e controla as **transações** (`@Transactional`);
- o **dirty checking** grava as alterações automaticamente: em `devolver`, basta mudar o objeto e o `UPDATE`
  é feito no fim da transação, sem chamar `save`;
- os relacionamentos são carregados **sob demanda** (`FetchType.LAZY`).

## Como executar

### Jeito rápido no Windows (um clique)

1. Instale os programas (uma vez só), no PowerShell ou no Prompt de Comando:

   ```powershell
   winget install EclipseAdoptium.Temurin.26.JDK
   winget install OpenJS.NodeJS.LTS
   winget install Oracle.MySQL
   winget install Git.Git
   ```

   Feche e abra o terminal depois de instalar. O Maven **não** precisa ser instalado: o projeto usa o Maven Wrapper (`backend/mvnw.cmd`).

2. Baixe o projeto:

   ```powershell
   git clone https://github.com/igorschiniegoski/biblioteca-fullstack-poo.git
   ```

   Ou, no GitHub, use **Code → Download ZIP** e extraia.

3. Dê dois cliques em **`scripts\windows\iniciar.bat`**. O script:
   - cria um MySQL próprio do projeto na **porta 3307**, sem senha de root e com os dados em `%USERPROFILE%\biblioteca-mysql`, sem interferir em outro MySQL ou XAMPP que já esteja instalado;
   - executa o `schema.sql` e o `dados-iniciais.sql`, recriando os dados de exemplo a cada execução;
   - abre o backend e o frontend em duas janelas e o navegador em http://localhost:5173.

   Na primeira vez, ele baixa o Maven e as dependências e pode levar alguns minutos. Use **`scripts\windows\parar.bat`** para desligar tudo.

### Pré-requisitos

- JDK 26 e Maven 3.9+
- Node.js 20+ e npm
- MySQL 8 rodando em `localhost:3306`

### 1. Criar e configurar o banco

```bash
# cria o banco "biblioteca" e as tabelas
mysql -u root -p < database/schema.sql

# (opcional) carrega os dados de exemplo
mysql -u root -p --default-character-set=utf8mb4 biblioteca < database/dados-iniciais.sql
```

Crie um usuário só para a aplicação (no cliente `mysql`, como root):

```sql
CREATE USER 'biblioteca'@'localhost' IDENTIFIED BY 'biblioteca';
GRANT ALL PRIVILEGES ON biblioteca.* TO 'biblioteca'@'localhost';
```

### 2. Configurar a conexão do Spring Boot

A conexão está em `backend/src/main/resources/application.properties` e lê variáveis de ambiente, com valores
padrão para desenvolvimento local:

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/biblioteca?serverTimezone=America/Sao_Paulo` |
| `DB_USER` | `biblioteca` |
| `DB_PASSWORD` | `biblioteca` |

Para usar outro usuário ou senha, defina as variáveis antes de rodar. Nenhuma senha real fica no repositório.

```powershell
# PowerShell
$env:DB_USER="meu_usuario"; $env:DB_PASSWORD="minha_senha"
```

### 3. Executar o backend (porta 8080)

```bash
cd backend
mvn spring-boot:run
```

### 4. Executar o frontend (porta 5173)

```bash
cd frontend
npm install
npm run dev
```

Acesse **http://localhost:5173**. O Vite repassa as chamadas `/api` para o backend na porta 8080
(veja `frontend/vite.config.js`).

### 5. Rodar os testes

```bash
cd backend
mvn test
```

Os testes usam H2 em memória e **não precisam do MySQL**.

## Testes automatizados

| Classe | O que verifica |
|---|---|
| `LeitorTest` | limite de 3 empréstimos; bloqueio por atraso; bloqueio por multa; quitar multas desbloqueia; validação de dados |
| `EmprestimoTest` | prazo de 14 dias; devolução sem multa; multa de R$ 2,00/dia; não devolver duas vezes; renovação única; não renovar atrasado |
| `LivroExemplarTest` | não emprestar exemplar emprestado ou baixado; não dar baixa em exemplar emprestado; contagem de disponíveis; validação de ISBN |
| `BibliotecaApiTest` | integração HTTP completa (Controller → Service → Domain → Repository → JPA → banco): fluxo de empréstimo e devolução, 400, 404, 409, PUT e DELETE |

## Funcionalidades do frontend

- **Empréstimos**: registrar empréstimo escolhendo leitor e exemplar disponível; listar ativos, devolvidos ou todos;
  devolver; renovar. Empréstimos atrasados aparecem destacados.
- **Leitores**: cadastrar, editar, excluir; ver situação (em atraso, multa pendente); pagar multas; ver o histórico.
- **Livros**: cadastrar, editar, excluir; gerenciar exemplares (adicionar, dar baixa) e ver a disponibilidade.
- As mensagens de erro do backend (400/404/409) aparecem na tela, incluindo o erro de cada campo do formulário.
