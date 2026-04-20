import type { Alerta } from '../types/alerta.js'

export const ALERTAS_ACTIVAS: Alerta[] = [
  {
    id: '1',
    enfermedad: 'Dengue hemorrágico',
    severidad: 'alto',
    codigoPostal: '64000',
    casosConfirmados: 47,
    tendenciaSemanal: 12,
    pruebasSugeridas: ['PCR dengue', 'Biometría hemática', 'Plaquetas'],
    perfilUrl: '/epidemiologia/dengue-hemorragico',
  },
  {
    id: '2',
    enfermedad: 'Influenza A (H3N2)',
    severidad: 'medio',
    codigoPostal: '64010',
    casosConfirmados: 23,
    tendenciaSemanal: -3,
    pruebasSugeridas: ['Panel viral respiratorio', 'Antígeno influenza'],
    perfilUrl: '/epidemiologia/influenza-a',
  },
  {
    id: '3',
    enfermedad: 'COVID-19',
    severidad: 'bajo',
    codigoPostal: '64020',
    casosConfirmados: 8,
    tendenciaSemanal: -2,
    pruebasSugeridas: ['Prueba rápida antígenos'],
    perfilUrl: '/epidemiologia/covid-19',
  },
  {
    id: '4',
    enfermedad: 'Leptospirosis',
    severidad: 'alto',
    codigoPostal: '44100',
    casosConfirmados: 15,
    tendenciaSemanal: 5,
    pruebasSugeridas: ['MAT leptospira', 'PCR leptospira', 'BH con diferencial'],
    perfilUrl: '/epidemiologia/leptospirosis',
  },
  {
    id: '5',
    enfermedad: 'Hepatitis A',
    severidad: 'medio',
    codigoPostal: '44110',
    casosConfirmados: 11,
    tendenciaSemanal: 2,
    pruebasSugeridas: ['Anti-VHA IgM', 'Pruebas de función hepática'],
    perfilUrl: '/epidemiologia/hepatitis-a',
  },
  {
    id: '6',
    enfermedad: 'Sarampión',
    severidad: 'alto',
    codigoPostal: '06600',
    casosConfirmados: 6,
    tendenciaSemanal: 3,
    pruebasSugeridas: ['IgM sarampión', 'PCR viral', 'Cultivo viral'],
    perfilUrl: '/epidemiologia/sarampion',
  },
]

const ORDEN_SEVERIDAD: Record<Alerta['severidad'], number> = {
  alto: 0,
  medio: 1,
  bajo: 2,
}

export function buscarAlertasPorCP(codigoPostal: string): Alerta[] {
  const prefijo = codigoPostal.slice(0, 3)
  return ALERTAS_ACTIVAS
    .filter((a) => a.codigoPostal.startsWith(prefijo))
    .sort((a, b) => ORDEN_SEVERIDAD[a.severidad] - ORDEN_SEVERIDAD[b.severidad])
}
