import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Aviso, { useAviso } from '../components/Aviso.jsx';

const VAZIO = { isbn: '', titulo: '', autor: '', anoPublicacao: '' };

export default function LivrosPage() {
  const [livros, setLivros] = useState([]);
  const [form, setForm] = useState(VAZIO);
  const [editandoId, setEditandoId] = useState(null);
  const [selecionadoId, setSelecionadoId] = useState(null);
  const [codigoExemplar, setCodigoExemplar] = useState('');
  const { aviso, setAviso, executar } = useAviso();

  async function carregar() {
    setLivros(await api.livros.listar());
  }

  useEffect(() => {
    carregar().catch((e) => setAviso({ tipo: 'erro', texto: e.message }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selecionado = livros.find((l) => l.id === selecionadoId);

  function alterar(campo, valor) {
    setForm((f) => ({ ...f, [campo]: valor }));
  }

  async function salvar(evento) {
    evento.preventDefault();
    const ano = form.anoPublicacao === '' ? null : Number(form.anoPublicacao);
    const ok = editandoId
      ? await executar(
          () => api.livros.atualizar(editandoId, { titulo: form.titulo, autor: form.autor, anoPublicacao: ano }),
          'Livro atualizado.',
        )
      : await executar(() => api.livros.cadastrar({ ...form, anoPublicacao: ano }), 'Livro cadastrado.');
    if (ok) {
      cancelar();
      await carregar();
    }
  }

  function editar(livro) {
    setEditandoId(livro.id);
    setForm({ isbn: livro.isbn, titulo: livro.titulo, autor: livro.autor, anoPublicacao: livro.anoPublicacao ?? '' });
  }

  function cancelar() {
    setEditandoId(null);
    setForm(VAZIO);
  }

  async function excluir(livro) {
    if (!window.confirm(`Excluir o livro "${livro.titulo}" e seus exemplares?`)) return;
    if (await executar(() => api.livros.excluir(livro.id), 'Livro excluído.')) {
      if (selecionadoId === livro.id) setSelecionadoId(null);
      await carregar();
    }
  }

  async function adicionarExemplar(evento) {
    evento.preventDefault();
    const ok = await executar(
      () => api.livros.adicionarExemplar(selecionadoId, codigoExemplar),
      `Exemplar ${codigoExemplar.toUpperCase()} adicionado.`,
    );
    if (ok) {
      setCodigoExemplar('');
      await carregar();
    }
  }

  async function darBaixa(exemplar) {
    if (!window.confirm(`Dar baixa no exemplar ${exemplar.codigo}? Ele não poderá mais ser emprestado.`)) return;
    if (await executar(() => api.livros.darBaixaExemplar(exemplar.id), `Exemplar ${exemplar.codigo} baixado.`)) {
      await carregar();
    }
  }

  return (
    <section>
      <h2>Livros</h2>
      <Aviso aviso={aviso} onFechar={() => setAviso(null)} />

      <form className="cartao formulario" onSubmit={salvar}>
        <h3>{editandoId ? 'Editar livro' : 'Novo livro'}</h3>
        <label>
          ISBN
          <input
            value={form.isbn}
            onChange={(e) => alterar('isbn', e.target.value)}
            disabled={!!editandoId}
            placeholder="978-85-..."
            maxLength={17}
          />
        </label>
        <label>
          Título
          <input value={form.titulo} onChange={(e) => alterar('titulo', e.target.value)} maxLength={200} />
        </label>
        <label>
          Autor
          <input value={form.autor} onChange={(e) => alterar('autor', e.target.value)} maxLength={150} />
        </label>
        <label className="curto">
          Ano
          <input
            type="number"
            value={form.anoPublicacao}
            onChange={(e) => alterar('anoPublicacao', e.target.value)}
          />
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
            <th>Título</th>
            <th>Autor</th>
            <th>Ano</th>
            <th>ISBN</th>
            <th>Disponíveis</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {livros.length === 0 && (
            <tr>
              <td colSpan="6" className="vazio">Nenhum livro cadastrado.</td>
            </tr>
          )}
          {livros.map((l) => (
            <tr key={l.id} className={l.id === selecionadoId ? 'selecionado' : ''}>
              <td>{l.titulo}</td>
              <td>{l.autor}</td>
              <td>{l.anoPublicacao ?? '—'}</td>
              <td className="codigo">{l.isbn}</td>
              <td>
                {l.exemplaresDisponiveis} de {l.totalExemplares}
              </td>
              <td className="acoes">
                <button className="secundario" onClick={() => setSelecionadoId(l.id)}>Exemplares</button>
                <button className="secundario" onClick={() => editar(l)}>Editar</button>
                <button className="perigo" onClick={() => excluir(l)}>Excluir</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selecionado && (
        <div className="cartao">
          <div className="titulo-cartao">
            <h3>Exemplares de “{selecionado.titulo}”</h3>
            <button className="secundario" onClick={() => setSelecionadoId(null)}>Fechar</button>
          </div>
          <form className="formulario" onSubmit={adicionarExemplar}>
            <label>
              Código do novo exemplar
              <input value={codigoExemplar} onChange={(e) => setCodigoExemplar(e.target.value)} maxLength={30} />
            </label>
            <button type="submit">Adicionar exemplar</button>
          </form>
          {selecionado.exemplares.length === 0 ? (
            <p className="vazio">Nenhum exemplar cadastrado.</p>
          ) : (
            <ul className="lista">
              {selecionado.exemplares.map((ex) => (
                <li key={ex.id}>
                  <span className="codigo">{ex.codigo}</span>{' '}
                  <span className={`etiqueta ${ex.situacao === 'DISPONIVEL' ? 'ok' : ex.situacao === 'BAIXADO' ? 'erro' : ''}`}>
                    {ex.situacao.toLowerCase()}
                  </span>
                  {ex.situacao !== 'BAIXADO' && (
                    <button className="link" onClick={() => darBaixa(ex)}>dar baixa</button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
