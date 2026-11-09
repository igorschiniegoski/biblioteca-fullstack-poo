import { useEffect, useState } from 'react';
import { api, formatarData, formatarDinheiro } from '../api.js';
import Aviso, { useAviso } from '../components/Aviso.jsx';

export default function EmprestimosPage() {
  const [emprestimos, setEmprestimos] = useState([]);
  const [leitores, setLeitores] = useState([]);
  const [livros, setLivros] = useState([]);
  const [filtro, setFiltro] = useState('ATIVO');
  const [leitorId, setLeitorId] = useState('');
  const [exemplarId, setExemplarId] = useState('');
  const { aviso, setAviso, executar } = useAviso();

  async function carregar(situacao = filtro) {
    const [lista, ls, lv] = await Promise.all([
      api.emprestimos.listar(situacao || undefined),
      api.leitores.listar(),
      api.livros.listar(),
    ]);
    setEmprestimos(lista);
    setLeitores(ls);
    setLivros(lv);
  }

  useEffect(() => {
    carregar(filtro).catch((e) => setAviso({ tipo: 'erro', texto: e.message }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtro]);

  // Apenas exemplares disponíveis podem ser escolhidos para empréstimo.
  const exemplaresDisponiveis = livros.flatMap((livro) =>
    livro.exemplares
      .filter((ex) => ex.situacao === 'DISPONIVEL')
      .map((ex) => ({ ...ex, titulo: livro.titulo })),
  );

  async function emprestar(evento) {
    evento.preventDefault();
    const ok = await executar(
      () => api.emprestimos.emprestar(leitorId ? Number(leitorId) : null, exemplarId ? Number(exemplarId) : null),
      'Empréstimo registrado.',
    );
    if (ok) {
      setExemplarId('');
      await carregar();
    }
  }

  async function acao(operacao, mensagem) {
    if (await executar(operacao, mensagem)) await carregar();
  }

  return (
    <section>
      <h2>Empréstimos</h2>
      <Aviso aviso={aviso} onFechar={() => setAviso(null)} />

      <form className="cartao formulario" onSubmit={emprestar}>
        <h3>Novo empréstimo</h3>
        <label>
          Leitor
          <select value={leitorId} onChange={(e) => setLeitorId(e.target.value)}>
            <option value="">Selecione…</option>
            {leitores.map((l) => (
              <option key={l.id} value={l.id}>
                {l.nome} — {l.emprestimosAtivos} ativo(s)
                {l.possuiAtraso ? ' · ATRASO' : ''}
                {Number(l.multasPendentes) > 0 ? ` · multa ${formatarDinheiro(l.multasPendentes)}` : ''}
              </option>
            ))}
          </select>
        </label>
        <label>
          Exemplar disponível
          <select value={exemplarId} onChange={(e) => setExemplarId(e.target.value)}>
            <option value="">Selecione…</option>
            {exemplaresDisponiveis.map((ex) => (
              <option key={ex.id} value={ex.id}>
                {ex.codigo} — {ex.titulo}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Emprestar</button>
        <p className="dica">Prazo de 14 dias · até 3 empréstimos por leitor · multa de R$ 2,00 por dia de atraso.</p>
      </form>

      <div className="barra">
        <label>
          Mostrar{' '}
          <select value={filtro} onChange={(e) => setFiltro(e.target.value)}>
            <option value="ATIVO">Ativos</option>
            <option value="DEVOLVIDO">Devolvidos</option>
            <option value="">Todos</option>
          </select>
        </label>
      </div>

      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Leitor</th>
            <th>Livro (exemplar)</th>
            <th>Empréstimo</th>
            <th>Devolver até</th>
            <th>Situação</th>
            <th>Multa</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {emprestimos.length === 0 && (
            <tr>
              <td colSpan="8" className="vazio">Nenhum empréstimo encontrado.</td>
            </tr>
          )}
          {emprestimos.map((e) => (
            <tr key={e.id} className={e.atrasado ? 'atrasado' : ''}>
              <td>{e.id}</td>
              <td>{e.leitorNome}</td>
              <td>
                {e.livroTitulo} <span className="codigo">{e.exemplarCodigo}</span>
              </td>
              <td>{formatarData(e.dataEmprestimo)}</td>
              <td>
                {formatarData(e.dataPrevistaDevolucao)}
                {e.renovacoes > 0 && <span className="etiqueta">renovado</span>}
              </td>
              <td>
                {e.situacao === 'DEVOLVIDO' ? (
                  <span className="etiqueta ok">devolvido em {formatarData(e.dataDevolucao)}</span>
                ) : e.atrasado ? (
                  <span className="etiqueta erro">{e.diasAtraso} dia(s) de atraso</span>
                ) : (
                  <span className="etiqueta">ativo</span>
                )}
              </td>
              <td>
                {Number(e.multa) > 0 ? (
                  <>
                    {formatarDinheiro(e.multa)} {e.multaPaga ? '(paga)' : '(pendente)'}
                  </>
                ) : (
                  '—'
                )}
              </td>
              <td className="acoes">
                {e.situacao === 'ATIVO' && (
                  <>
                    <button onClick={() => acao(() => api.emprestimos.devolver(e.id), 'Devolução registrada.')}>
                      Devolver
                    </button>
                    <button
                      className="secundario"
                      onClick={() => acao(() => api.emprestimos.renovar(e.id), 'Empréstimo renovado por mais 14 dias.')}
                    >
                      Renovar
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
