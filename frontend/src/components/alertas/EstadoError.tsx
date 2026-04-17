interface EstadoErrorProps {
  onReintentar: () => void
}

export function EstadoError({ onReintentar }: EstadoErrorProps) {
  return (
    <div className="flex flex-col items-center justify-center py-6 px-3 text-center">
      <span className="text-2xl mb-3" aria-hidden="true">⚠️</span>
      <p className="text-red-400 font-semibold text-sm mb-2">No se pudo cargar</p>
      <p className="text-slate-500 text-xs leading-relaxed mb-4">
        Error al consultar el contexto epidemiológico.
      </p>
      <button
        onClick={onReintentar}
        className="bg-purple-900/30 text-purple-400 border border-purple-700 rounded-md px-4 py-1.5 text-xs hover:bg-purple-900/50 transition-colors"
      >
        Reintentar
      </button>
    </div>
  )
}
