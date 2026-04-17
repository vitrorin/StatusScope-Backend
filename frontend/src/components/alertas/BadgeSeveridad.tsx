interface BadgeSeveridadProps {
  severidad: 'alto' | 'medio' | 'bajo'
}

const estilos: Record<BadgeSeveridadProps['severidad'], string> = {
  alto: 'bg-red-500/10 text-red-400 border border-red-500',
  medio: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500',
  bajo: 'bg-blue-500/10 text-blue-400 border border-blue-400',
}

const etiquetas: Record<BadgeSeveridadProps['severidad'], string> = {
  alto: 'ALTO',
  medio: 'MEDIO',
  bajo: 'BAJO',
}

export function BadgeSeveridad({ severidad }: BadgeSeveridadProps) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${estilos[severidad]}`}>
      {etiquetas[severidad]}
    </span>
  )
}
