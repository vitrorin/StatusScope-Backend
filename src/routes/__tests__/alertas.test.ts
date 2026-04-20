import { describe, it, expect } from 'vitest'
import request from 'supertest'
import app from '../../index.js'

describe('GET /api/alertas', () => {
  it('devuelve 400 si no se envía codigoPostal', async () => {
    const res = await request(app).get('/api/alertas')
    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('error')
  })

  it('devuelve 400 si codigoPostal tiene menos de 3 caracteres', async () => {
    const res = await request(app).get('/api/alertas?codigoPostal=64')
    expect(res.status).toBe(400)
  })

  it('devuelve 200 con alertas para un CP válido con coincidencias', async () => {
    const res = await request(app).get('/api/alertas?codigoPostal=64000')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('alertas')
    expect(res.body.alertas.length).toBeGreaterThan(0)
    expect(res.body).toHaveProperty('totalEncontradas')
    expect(res.body).toHaveProperty('codigoPostal', '64000')
  })

  it('devuelve lista vacía para CP sin coincidencias', async () => {
    const res = await request(app).get('/api/alertas?codigoPostal=99999')
    expect(res.status).toBe(200)
    expect(res.body.alertas).toHaveLength(0)
    expect(res.body.totalEncontradas).toBe(0)
  })

  it('responde al health check', async () => {
    const res = await request(app).get('/health')
    expect(res.status).toBe(200)
    expect(res.body.status).toBe('ok')
  })
})
