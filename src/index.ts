import express from 'express'
import cors from 'cors'
import alertasRouter from './routes/alertas.js'

const app = express()
const PORT = process.env.PORT ?? 3000

app.use(cors({ origin: 'http://localhost:5174' }))
app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

app.use('/api/alertas', alertasRouter)

app.listen(PORT, () => {
  console.log(`StatuScope API corriendo en http://localhost:${PORT}`)
})

export default app
