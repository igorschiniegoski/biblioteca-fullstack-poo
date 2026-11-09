import { useState } from 'react';

// Mostra a mensagem de sucesso ou de erro da última operação (incluindo os erros de cada campo).
export default function Aviso({ aviso, onFechar }) {
  if (!aviso) return null;
  const campos = Object.entries(aviso.campos ?? {});
  return (
    <div className={`aviso ${aviso.tipo}`} role={aviso.tipo === 'erro' ? 'alert' : 'status'}>
      <div>
        <strong>{aviso.texto}</strong>
        {campos.length > 0 && (
          <ul>
            {campos.map(([campo, msg]) => (
              <li key={campo}>{msg}</li>
            ))}
          </ul>
        )}
      </div>
      <button className="fechar" onClick={onFechar} aria-label="Fechar">×</button>
    </div>
  );
}

// Hook auxiliar: executa uma operação da API e registra sucesso/erro no aviso.
export function useAviso() {
  const [aviso, setAviso] = useState(null);

  async function executar(operacao, mensagemSucesso) {
    try {
      const resultado = await operacao();
      if (mensagemSucesso) setAviso({ tipo: 'sucesso', texto: mensagemSucesso });
      return resultado ?? true;
    } catch (erro) {
      setAviso({ tipo: 'erro', texto: erro.message, campos: erro.campos });
      return undefined;
    }
  }

  return { aviso, setAviso, executar };
}
