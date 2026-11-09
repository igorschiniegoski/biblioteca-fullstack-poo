// Camada de acesso à API REST. Todas as telas usam estas funções.
// Em caso de erro, lança um Error com a mensagem enviada pelo backend (ErroResponse).

async function requisicao(metodo, caminho, corpo) {
  const opcoes = { method: metodo, headers: {} };
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }

  let resposta;
  try {
    resposta = await fetch(`/api${caminho}`, opcoes);
  } catch {
    throw new Error('Não foi possível conectar à API. O backend está rodando?');
  }

  if (resposta.status === 204) return null;
  const dados = await resposta.json().catch(() => null);

  if (!resposta.ok) {
    const erro = new Error(dados?.mensagem ?? `Erro ${resposta.status}`);
    erro.status = resposta.status;
    erro.campos = dados?.campos ?? {};
    throw erro;
  }
  return dados;
}

export const api = {
  leitores: {
    listar: () => requisicao('GET', '/leitores'),
    cadastrar: (dados) => requisicao('POST', '/leitores', dados),
    atualizar: (id, dados) => requisicao('PUT', `/leitores/${id}`, dados),
    excluir: (id) => requisicao('DELETE', `/leitores/${id}`),
    historico: (id) => requisicao('GET', `/leitores/${id}/emprestimos`),
    pagarMultas: (id) => requisicao('POST', `/leitores/${id}/pagamento-multas`),
  },
  livros: {
    listar: () => requisicao('GET', '/livros'),
    cadastrar: (dados) => requisicao('POST', '/livros', dados),
    atualizar: (id, dados) => requisicao('PUT', `/livros/${id}`, dados),
    excluir: (id) => requisicao('DELETE', `/livros/${id}`),
    adicionarExemplar: (id, codigo) => requisicao('POST', `/livros/${id}/exemplares`, { codigo }),
    darBaixaExemplar: (exemplarId) => requisicao('PATCH', `/exemplares/${exemplarId}/baixa`),
  },
  emprestimos: {
    listar: (situacao) => requisicao('GET', situacao ? `/emprestimos?situacao=${situacao}` : '/emprestimos'),
    emprestar: (leitorId, exemplarId) => requisicao('POST', '/emprestimos', { leitorId, exemplarId }),
    devolver: (id) => requisicao('PATCH', `/emprestimos/${id}/devolucao`),
    renovar: (id) => requisicao('PATCH', `/emprestimos/${id}/renovacao`),
  },
};

export const formatarData = (iso) => (iso ? iso.split('-').reverse().join('/') : '—');

export const formatarDinheiro = (valor) =>
  Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
