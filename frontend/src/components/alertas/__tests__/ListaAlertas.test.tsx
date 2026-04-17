import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ListaAlertas } from '../ListaAlertas'
import { Alerta } from '../../../types/alerta'

const alertas: Alerta[] = [
  {
    id: '1',
    enfermedad: 'Sarampión',
    severidad: 'alto',
    codigoPostal: '64000',
    casosConfirmados: 23,
    tendenciaSemanal: 8,
    pruebasSugeridas: ['IgM sarampión'],
    perfilUrl: '/epidemiologia/sarampion',
  },
  {
    id: '2',
    enfermedad: 'Dengue',
    severidad: 'medio',
    codigoPostal: '64010',
    casosConfirmados: 12,
    tendenciaSemanal: 3,
    pruebasSugeridas: ['NS1 dengue'],
    perfilUrl: '/epidemiologia/dengue',
  },
]

describe('ListaAlertas', () => {
  it('renderiza una tarjeta por cada alerta', () => {
    render(<ListaAlertas alertas={alertas} />)
    expect(screen.getByText('Sarampión')).toBeInTheDocument()
    expect(screen.getByText('Dengue')).toBeInTheDocument()
  })

  it('no renderiza tarjetas cuando la lista está vacía', () => {
    const { container } = render(<ListaAlertas alertas={[]} />)
    expect(container.firstChild?.childNodes.length).toBe(0)
  })
})
