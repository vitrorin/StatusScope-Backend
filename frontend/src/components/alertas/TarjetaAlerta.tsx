import { Alerta } from '../../types/alerta'
import { BadgeSeveridad } from './BadgeSeveridad'

interface TarjetaAlertaProps {
  alerta: Alerta
}

const borderColor: Record<Alerta['severidad'], string> = {
  alto: 'border-l-red-500',
  medio: 'border-l-yellow-500',
  bajo: 'border-l-blue-400',
}

const bgColor: Record<Alerta['severidad'], string> = {
  alto: 'bg-purple-900/30',
  medio: 'bg-blue-900/20',
  bajo: 'bg-slate-800/30',
}

const nombreColor: Record<Alerta['severidad'], string> = {
  alto: 'text-yellow-400',
  medio: 'text-blue-400',
  bajo: 'text-slate-300',
}

export function TarjetaAlerta({ alerta }: TarjetaAlertaProps) {
  const tendenciaTexto =
    alerta.tendenciaSemanal >= 0
      ? `↑ +${alerta.tendenciaSemanal} vs semana anterior`
      : `↓ ${alerta.tendenciaSemanal} vs semana anterior`

  return (
    <div
      className={`${bgColor[alerta.severidad]} border-l-4 ${borderColor[alerta.severidad]} rounded-md p-3`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className={`font-bold text-sm ${nombreColor[alerta.severidad]}`}>
          {alerta.enfermedad}
        </span>
        <BadgeSeveridad severidad={alerta.severidad} />
      </div>

      <p className="text-slate-300 text-xs mb-1">
        📍 CP {alerta.codigoPostal} · {alerta.casosConfirmados} casos confirmados
      </p>
      <p className="text-green-400 text-xs mb-3">{tendenciaTexto}</p>

      <div className="border-t border-purple-800/30 pt-2 mb-2">
        <p className="text-slate-500 text-xs uppercase tracking-wide mb-1">
          Pruebas sugeridas
        </p>
        <p className="text-slate-300 text-xs">
          {alerta.pruebasSugeridas.map((p) => `• ${p}`).join('  ')}
        </p>
      </div>

      <div className="text-right">
        <a
          href={alerta.perfilUrl}
          className="text-purple-400 text-xs underline hover:text-purple-300"
        >
          Ver perfil epidemiológico →
        </a>
      </div>
    </div>
  )
}
