import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TarjetaAlerta } from '../TarjetaAlerta'
import { Alerta } from '../../../types/alerta'

const alertaBase: Alerta = {
  id: '1',
  enfermedad: 'Sarampión',
  severidad: 'alto',
  codigoPostal: '64000',
  casosConfirmados: 23,
  tendenciaSemanal: 8,
  pruebasSugeridas: ['IgM sarampión', 'PCR viral'],
  perfilUrl: '/epidemiologia/sarampion',
}

describe('TarjetaAlerta', () => {
  it('muestra el nombre de la enfermedad', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    expect(screen.getByText('Sarampión')).toBeInTheDocument()
  })

  it('muestra el código postal y casos confirmados', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    expect(screen.getByText(/CP 64000/)).toBeInTheDocument()
    expect(screen.getByText(/23 casos confirmados/)).toBeInTheDocument()
  })

  it('muestra tendencia positiva con flecha arriba', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    expect(screen.getByText(/↑ \+8 vs semana anterior/)).toBeInTheDocument()
  })

  it('muestra tendencia negativa con flecha abajo', () => {
    render(<TarjetaAlerta alerta={{ ...alertaBase, tendenciaSemanal: -3 }} />)
    expect(screen.getByText(/↓ -3 vs semana anterior/)).toBeInTheDocument()
  })

  it('muestra las pruebas sugeridas', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    expect(screen.getByText(/IgM sarampión/)).toBeInTheDocument()
    expect(screen.getByText(/PCR viral/)).toBeInTheDocument()
  })

  it('muestra el enlace al perfil epidemiológico', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    const enlace = screen.getByRole('link', { name: /Ver perfil epidemiológico/i })
    expect(enlace).toHaveAttribute('href', '/epidemiologia/sarampion')
  })

  it('muestra badge de severidad alto', () => {
    render(<TarjetaAlerta alerta={alertaBase} />)
    expect(screen.getByText('ALTO')).toBeInTheDocument()
  })

  it('aplica borde rojo para severidad alto', () => {
    const { container } = render(<TarjetaAlerta alerta={alertaBase} />)
    expect(container.firstChild).toHaveClass('border-l-red-500')
  })

  it('aplica borde amarillo para severidad medio', () => {
    const { container } = render(
      <TarjetaAlerta alerta={{ ...alertaBase, severidad: 'medio' }} />
    )
    expect(container.firstChild).toHaveClass('border-l-yellow-500')
  })
})
