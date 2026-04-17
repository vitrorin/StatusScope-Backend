export interface Alerta {
  id: string
  enfermedad: string
  severidad: 'alto' | 'medio' | 'bajo'
  codigoPostal: string
  casosConfirmados: number
  tendenciaSemanal: number
  pruebasSugeridas: string[]
  perfilUrl: string
}

export interface PanelAlertasProps {
  estado: 'idle' | 'cargando' | 'con_alertas' | 'sin_alertas' | 'error'
  alertas: Alerta[]
  onReintentar: () => void
}
