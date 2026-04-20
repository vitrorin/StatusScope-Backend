import { buscarAlertasPorCP } from '../data/alertas.js'
import type { Alerta } from '../types/alerta.js'

export interface ResultadoDeteccion {
  alertas: Alerta[]
  totalEncontradas: number
  codigoPostal: string
}

export function detectarAlertas(codigoPostal: string): ResultadoDeteccion {
  const alertas = buscarAlertasPorCP(codigoPostal)
  return {
    alertas,
    totalEncontradas: alertas.length,
    codigoPostal,
  }
}
