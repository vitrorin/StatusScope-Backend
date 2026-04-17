export function EstadoSinAlertas() {
  return (
    <div className="flex flex-col items-center justify-center py-8 px-3 text-center">
      <span className="text-3xl mb-3" aria-hidden="true">✓</span>
      <p className="text-green-400 font-semibold text-sm mb-2">Sin alertas activas</p>
      <p className="text-slate-500 text-xs leading-relaxed">
        No se detectaron brotes en la zona que coincidan con los síntomas registrados.
      </p>
    </div>
  )
}
