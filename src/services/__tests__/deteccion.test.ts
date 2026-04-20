import { describe, it, expect } from 'vitest'
import { detectarAlertas } from '../deteccion.js'

describe('detectarAlertas', () => {
  it('devuelve alertas para un CP con coincidencias', () => {
    const resultado = detectarAlertas('64000')
    expect(resultado.alertas.length).toBeGreaterThan(0)
    expect(resultado.totalEncontradas).toBe(resultado.alertas.length)
    expect(resultado.codigoPostal).toBe('64000')
  })

  it('devuelve lista vacía para un CP sin coincidencias', () => {
    const resultado = detectarAlertas('99999')
    expect(resultado.alertas).toHaveLength(0)
    expect(resultado.totalEncontradas).toBe(0)
  })

  it('filtra por los primeros 3 dígitos del CP', () => {
    const r1 = detectarAlertas('64000')
    const r2 = detectarAlertas('64099')
    expect(r1.alertas.map((a) => a.id)).toEqual(r2.alertas.map((a) => a.id))
  })

  it('ordena alertas por severidad: alto primero', () => {
    const resultado = detectarAlertas('64000')
    const severidades = resultado.alertas.map((a) => a.severidad)
    const orden = ['alto', 'medio', 'bajo']
    for (let i = 0; i < severidades.length - 1; i++) {
      expect(orden.indexOf(severidades[i])).toBeLessThanOrEqual(
        orden.indexOf(severidades[i + 1])
      )
    }
  })

  it('incluye los campos requeridos en cada alerta', () => {
    const resultado = detectarAlertas('64000')
    const alerta = resultado.alertas[0]
    expect(alerta).toHaveProperty('id')
    expect(alerta).toHaveProperty('enfermedad')
    expect(alerta).toHaveProperty('severidad')
    expect(alerta).toHaveProperty('codigoPostal')
    expect(alerta).toHaveProperty('casosConfirmados')
    expect(alerta).toHaveProperty('tendenciaSemanal')
    expect(alerta).toHaveProperty('pruebasSugeridas')
    expect(alerta).toHaveProperty('perfilUrl')
  })
})
