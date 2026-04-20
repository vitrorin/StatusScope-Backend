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
