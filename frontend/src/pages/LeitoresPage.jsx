import { useEffect, useState } from 'react';
import { api, formatarData, formatarDinheiro } from '../api.js';
import Aviso, { useAviso } from '../components/Aviso.jsx';

const VAZIO = { nome: '', email: '', telefone: '' };

export default function LeitoresPage() {
  const [leitores, setLeitores] = useState([]);
  const [form, setForm] = useState(VAZIO);
  const [editandoId, setEditandoId] = useState(null);
  const [historico, setHistorico] = useState(null);
  const { aviso, setAviso, executar } = useAviso();

  async function carregar() {
    setLeitores(await api.leitores.listar());
  }

  useEffect(() => {
    carregar().catch((e) => setAviso({ tipo: 'erro', texto: e.message }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function alterar(campo, valor) {
    setForm((f) => ({ ...f, [campo]: valor }));
  }

  async function salvar(evento) {
    evento.preventDefault();
    const dados = { ...form, telefone: form.telefone || null };
    const ok = editandoId
      ? await executar(() => api.leitores.atualizar(editandoId, dados), 'Leitor atualizado.')
      : await executar(() => api.leitores.cadastrar(dados), 'Leitor cadastrado.');
    if (ok) {
      cancelar();
      await carregar();
    }
  }

  function editar(leitor) {
    setEditandoId(leitor.id);
    setForm({ nome: leitor.nome, email: leitor.email, telefone: leitor.telefone ?? '' });
  }

  function cancelar() {
    setEditandoId(null);
    setForm(VAZIO);
  }

  async function excluir(leitor) {
    if (!window.confirm(`Excluir o leitor ${leitor.nome}?`)) return;
    if (await executar(() => api.leitores.excluir(leitor.id), 'Leitor excluído.')) {
      if (historico?.leitor.id === leitor.id) setHistorico(null);
      await carregar();
    }
  }

  async function pagar(leitor) {
    const pagamento = await executar(() => api.leitores.pagarMultas(leitor.id));
    if (pagamento) {
      setAviso({ tipo: 'sucesso', texto: `Multas quitadas: ${formatarDinheiro(pagamento.valorPago)}.` });
      await carregar();
    }
  }

  async function verHistorico(leitor) {
    const lista = await executar(() => api.leitores.historico(leitor.id));
    if (lista) setHistorico({ leitor, lista });
  }

  return (
    <section>
      <h2>Leitores</h2>
      <Aviso aviso={aviso} onFechar={() => setAviso(null)} />

      <form className="cartao formulario" onSubmit={salvar}>
        <h3>{editandoId ? 'Editar leitor' : 'Novo leitor'}</h3>
        <label>
          Nome
          <input value={form.nome} onChange={(e) => alterar('nome', e.target.value)} maxLength={100} />
        </label>
        <label>
          E-mail
          <input value={form.email} onChange={(e) => alterar('email', e.target.value)} maxLength={150} />
        </label>
        <label>
          Telefone
          <input value={form.telefone} onChange={(e) => alterar('telefone', e.target.value)} maxLength={20} />
        </label>
        <button type="submit">{editandoId ? 'Salvar' : 'Cadastrar'}</button>
        {editandoId && (
          <button type="button" className="secundario" onClick={cancelar}>
            Cancelar
          </button>
        )}
      </form>

      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>E-mail</th>
            <th>Telefone</th>
            <th>Ativos</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {leitores.length === 0 && (
            <tr>
              <td colSpan="6" className="vazio">Nenhum leitor cadastrado.</td>
            </tr>
          )}
          {leitores.map((l) => (
            <tr key={l.id}>
              <td>{l.nome}</td>
              <td>{l.email}</td>
              <td>{l.telefone ?? '—'}</td>
              <td>{l.emprestimosAtivos} / 3</td>
              <td>
                {l.possuiAtraso && <span className="etiqueta erro">em atraso</span>}
                {Number(l.multasPendentes) > 0 && (
                  <span className="etiqueta erro">multa {formatarDinheiro(l.multasPendentes)}</span>
                )}
                {!l.possuiAtraso && Number(l.multasPendentes) === 0 && <span className="etiqueta ok">regular</span>}
              </td>
              <td className="acoes">
                <button className="secundario" onClick={() => verHistorico(l)}>Histórico</button>
                <button className="secundario" onClick={() => editar(l)}>Editar</button>
                {Number(l.multasPendentes) > 0 && <button onClick={() => pagar(l)}>Pagar multas</button>}
                <button className="perigo" onClick={() => excluir(l)}>Excluir</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {historico && (
        <div className="cartao">
          <div className="titulo-cartao">
            <h3>Histórico de {historico.leitor.nome}</h3>
            <button className="secundario" onClick={() => setHistorico(null)}>Fechar</button>
          </div>
          {historico.lista.length === 0 ? (
            <p className="vazio">Nenhum empréstimo.</p>
          ) : (
            <ul className="lista">
              {historico.lista.map((e) => (
                <li key={e.id}>
                  <strong>{e.livroTitulo}</strong> ({e.exemplarCodigo}) — {formatarData(e.dataEmprestimo)} →{' '}
                  {e.situacao === 'DEVOLVIDO' ? `devolvido em ${formatarData(e.dataDevolucao)}` : 'em aberto'}
                  {Number(e.multa) > 0 && ` · multa ${formatarDinheiro(e.multa)} ${e.multaPaga ? '(paga)' : '(pendente)'}`}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
