import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { EstadoSinAlertas } from '../EstadoSinAlertas'

describe('EstadoSinAlertas', () => {
  it('muestra el mensaje "Sin alertas activas"', () => {
    render(<EstadoSinAlertas />)
    expect(screen.getByText('Sin alertas activas')).toBeInTheDocument()
  })

  it('muestra la descripción de la ausencia de brotes', () => {
    render(<EstadoSinAlertas />)
    expect(
      screen.getByText(/No se detectaron brotes en la zona/i)
    ).toBeInTheDocument()
  })
})
