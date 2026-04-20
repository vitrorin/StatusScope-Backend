import { Router } from 'express'
import { detectarAlertas } from '../services/deteccion.js'

const router = Router()

router.get('/', (req, res) => {
  const { codigoPostal } = req.query

  if (!codigoPostal || typeof codigoPostal !== 'string' || codigoPostal.trim().length < 3) {
    res.status(400).json({
      error: 'Se requiere codigoPostal con al menos 3 dígitos',
    })
    return
  }

  const resultado = detectarAlertas(codigoPostal.trim())
  res.json(resultado)
})

export default router
