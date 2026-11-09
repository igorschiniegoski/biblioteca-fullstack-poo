import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import EmprestimosPage from './pages/EmprestimosPage.jsx';
import LeitoresPage from './pages/LeitoresPage.jsx';
import LivrosPage from './pages/LivrosPage.jsx';

export default function App() {
  return (
    <div className="app">
      <header className="topo">
        <h1>📚 Biblioteca</h1>
        <nav>
          <NavLink to="/emprestimos">Empréstimos</NavLink>
          <NavLink to="/leitores">Leitores</NavLink>
          <NavLink to="/livros">Livros</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/emprestimos" replace />} />
          <Route path="/emprestimos" element={<EmprestimosPage />} />
          <Route path="/leitores" element={<LeitoresPage />} />
          <Route path="/livros" element={<LivrosPage />} />
          <Route path="*" element={<p>Página não encontrada.</p>} />
        </Routes>
      </main>
    </div>
  );
}
