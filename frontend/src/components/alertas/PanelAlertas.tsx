import { PanelAlertasProps } from '../../types/alerta'
import { EstadoCargando } from './EstadoCargando'
import { EstadoSinAlertas } from './EstadoSinAlertas'
import { EstadoError } from './EstadoError'
import { ListaAlertas } from './ListaAlertas'

export function PanelAlertas({ estado, alertas, onReintentar }: PanelAlertasProps) {
  if (estado === 'idle') return null

  return (
    <aside className="w-64 flex-shrink-0 bg-purple-950/60 border border-purple-800 rounded-xl p-4">
      <h2 className="text-purple-400 text-xs font-bold tracking-widest mb-4 uppercase">
        ⚠ Alertas Epidemiológicas
      </h2>
      {estado === 'cargando' && <EstadoCargando />}
      {estado === 'sin_alertas' && <EstadoSinAlertas />}
      {estado === 'error' && <EstadoError onReintentar={onReintentar} />}
      {estado === 'con_alertas' && <ListaAlertas alertas={alertas} />}
    </aside>
  )
}
