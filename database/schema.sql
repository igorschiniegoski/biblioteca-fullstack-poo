-- =====================================================================
-- Sistema de Biblioteca - estrutura do banco (MySQL 8)
-- Execute:  mysql -u root -p < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS biblioteca
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE biblioteca;

DROP TABLE IF EXISTS emprestimo;
DROP TABLE IF EXISTS exemplar;
DROP TABLE IF EXISTS livro;
DROP TABLE IF EXISTS leitor;

-- Pessoa que pega livros emprestados.
CREATE TABLE leitor (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nome          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    telefone      VARCHAR(20)  NULL,
    data_cadastro DATE         NOT NULL,
    CONSTRAINT pk_leitor PRIMARY KEY (id),
    CONSTRAINT uk_leitor_email UNIQUE (email)
) ENGINE = InnoDB;

-- Obra do acervo (dados bibliográficos).
CREATE TABLE livro (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    isbn           VARCHAR(13)  NOT NULL,
    titulo         VARCHAR(200) NOT NULL,
    autor          VARCHAR(150) NOT NULL,
    ano_publicacao INT          NULL,
    CONSTRAINT pk_livro PRIMARY KEY (id),
    CONSTRAINT uk_livro_isbn UNIQUE (isbn),
    CONSTRAINT ck_livro_ano CHECK (ano_publicacao IS NULL OR ano_publicacao >= 1450)
) ENGINE = InnoDB;

-- Cópia física de um livro. 1 livro : N exemplares.
CREATE TABLE exemplar (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    livro_id BIGINT      NOT NULL,
    codigo   VARCHAR(30) NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    CONSTRAINT pk_exemplar PRIMARY KEY (id),
    CONSTRAINT uk_exemplar_codigo UNIQUE (codigo),
    CONSTRAINT fk_exemplar_livro FOREIGN KEY (livro_id) REFERENCES livro (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_exemplar_situacao CHECK (situacao IN ('DISPONIVEL', 'EMPRESTADO', 'BAIXADO'))
) ENGINE = InnoDB;

-- Empréstimo de um exemplar para um leitor.
-- 1 leitor : N empréstimos  |  1 exemplar : N empréstimos (ao longo do tempo).
-- ON DELETE RESTRICT: não se apaga leitor/exemplar que tenha histórico.
CREATE TABLE emprestimo (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    leitor_id               BIGINT        NOT NULL,
    exemplar_id             BIGINT        NOT NULL,
    data_emprestimo         DATE          NOT NULL,
    data_prevista_devolucao DATE          NOT NULL,
    data_devolucao          DATE          NULL,
    renovacoes              INT           NOT NULL DEFAULT 0,
    situacao                VARCHAR(20)   NOT NULL DEFAULT 'ATIVO',
    multa                   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    multa_paga              BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_emprestimo PRIMARY KEY (id),
    CONSTRAINT fk_emprestimo_leitor FOREIGN KEY (leitor_id) REFERENCES leitor (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_emprestimo_exemplar FOREIGN KEY (exemplar_id) REFERENCES exemplar (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_emprestimo_situacao CHECK (situacao IN ('ATIVO', 'DEVOLVIDO')),
    CONSTRAINT ck_emprestimo_renovacoes CHECK (renovacoes BETWEEN 0 AND 1),
    CONSTRAINT ck_emprestimo_multa CHECK (multa >= 0),
    CONSTRAINT ck_emprestimo_prazo CHECK (data_prevista_devolucao > data_emprestimo),
    CONSTRAINT ck_emprestimo_devolucao CHECK (data_devolucao IS NULL OR data_devolucao >= data_emprestimo)
) ENGINE = InnoDB;

CREATE INDEX ix_emprestimo_situacao ON emprestimo (situacao);
