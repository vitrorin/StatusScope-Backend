export function EstadoCargando() {
  return (
    <div className="flex flex-col gap-3">
      <div className="animate-pulse bg-purple-900/40 rounded-lg p-3">
        <div className="h-3 bg-purple-800/60 rounded w-3/5 mb-2" />
        <div className="h-2 bg-purple-900/60 rounded w-4/5 mb-1" />
        <div className="h-2 bg-purple-900/60 rounded w-1/2" />
      </div>
      <div className="animate-pulse bg-blue-900/20 rounded-lg p-3">
        <div className="h-3 bg-blue-800/40 rounded w-1/2 mb-2" />
        <div className="h-2 bg-blue-900/30 rounded w-3/4 mb-1" />
        <div className="h-2 bg-blue-900/30 rounded w-2/5" />
      </div>
      <p className="text-center text-purple-600 text-xs mt-1">
        Analizando contexto epidemiológico...
      </p>
    </div>
  )
}
