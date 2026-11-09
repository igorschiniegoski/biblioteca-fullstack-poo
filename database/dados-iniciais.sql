-- Dados de exemplo para demonstração (rodar depois do schema.sql):
--   mysql -u root -p biblioteca < database/dados-iniciais.sql
SET NAMES utf8mb4;
USE biblioteca;

INSERT INTO leitor (id, nome, email, telefone, data_cadastro) VALUES
    (1, 'Ana Souza',      'ana.souza@email.com',   '(42) 99911-2233', CURRENT_DATE - INTERVAL 60 DAY),
    (2, 'Bruno Lima',     'bruno.lima@email.com',  '(42) 99822-3344', CURRENT_DATE - INTERVAL 45 DAY),
    (3, 'Carla Mendes',   'carla.mendes@email.com', NULL,             CURRENT_DATE - INTERVAL 10 DAY);

INSERT INTO livro (id, isbn, titulo, autor, ano_publicacao) VALUES
    (1, '9788535902778', 'Dom Casmurro',                     'Machado de Assis',    1899),
    (2, '9788576082675', 'Código Limpo',                     'Robert C. Martin',    2008),
    (3, '9788550804606', 'Use a Cabeça! Java',               'Kathy Sierra',        2005),
    (4, '9788575224182', 'Padrões de Projeto',               'Erich Gamma',         1994);

INSERT INTO exemplar (id, livro_id, codigo, situacao) VALUES
    (1, 1, 'DC-001',   'DISPONIVEL'),
    (2, 1, 'DC-002',   'EMPRESTADO'),
    (3, 2, 'CL-001',   'EMPRESTADO'),
    (4, 2, 'CL-002',   'DISPONIVEL'),
    (5, 3, 'JAVA-001', 'DISPONIVEL'),
    (6, 4, 'GOF-001',  'DISPONIVEL'),
    (7, 4, 'GOF-002',  'BAIXADO');

-- Ana: empréstimo ativo dentro do prazo.
-- Bruno: empréstimo ativo ATRASADO (fica bloqueado para novos empréstimos).
-- Carla: empréstimo já devolvido com 3 dias de atraso e multa pendente de R$ 6,00.
INSERT INTO emprestimo (leitor_id, exemplar_id, data_emprestimo, data_prevista_devolucao, data_devolucao,
                        renovacoes, situacao, multa, multa_paga) VALUES
    (1, 2, CURRENT_DATE - INTERVAL 3 DAY,  CURRENT_DATE + INTERVAL 11 DAY, NULL,                         0, 'ATIVO',     0.00, TRUE),
    (2, 3, CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE - INTERVAL 6 DAY,  NULL,                         0, 'ATIVO',     0.00, TRUE),
    (3, 5, CURRENT_DATE - INTERVAL 17 DAY, CURRENT_DATE - INTERVAL 3 DAY,  CURRENT_DATE,                 0, 'DEVOLVIDO', 6.00, FALSE);