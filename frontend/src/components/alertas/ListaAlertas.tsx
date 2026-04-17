import { Alerta } from '../../types/alerta'
import { TarjetaAlerta } from './TarjetaAlerta'

interface ListaAlertasProps {
  alertas: Alerta[]
}

export function ListaAlertas({ alertas }: ListaAlertasProps) {
  return (
    <div className="flex flex-col gap-3">
      {alertas.map((alerta) => (
        <TarjetaAlerta key={alerta.id} alerta={alerta} />
      ))}
    </div>
  )
}
