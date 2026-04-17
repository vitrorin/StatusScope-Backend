import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PanelAlertas } from '../PanelAlertas'
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
]

describe('PanelAlertas', () => {
  it('no renderiza nada en estado idle', () => {
    const { container } = render(
      <PanelAlertas estado="idle" alertas={[]} onReintentar={() => {}} />
    )
    expect(container.firstChild).toBeNull()
  })

  it('muestra skeleton en estado cargando', () => {
    render(<PanelAlertas estado="cargando" alertas={[]} onReintentar={() => {}} />)
    expect(
      screen.getByText('Analizando contexto epidemiológico...')
    ).toBeInTheDocument()
  })

  it('muestra mensaje de sin alertas en estado sin_alertas', () => {
    render(<PanelAlertas estado="sin_alertas" alertas={[]} onReintentar={() => {}} />)
    expect(screen.getByText('Sin alertas activas')).toBeInTheDocument()
  })

  it('muestra mensaje de error en estado error', () => {
    render(<PanelAlertas estado="error" alertas={[]} onReintentar={() => {}} />)
    expect(screen.getByText('No se pudo cargar')).toBeInTheDocument()
  })

  it('llama onReintentar desde el estado error', () => {
    const onReintentar = vi.fn()
    render(<PanelAlertas estado="error" alertas={[]} onReintentar={onReintentar} />)
    fireEvent.click(screen.getByRole('button', { name: /reintentar/i }))
    expect(onReintentar).toHaveBeenCalledTimes(1)
  })

  it('muestra la lista de alertas en estado con_alertas', () => {
    render(
      <PanelAlertas estado="con_alertas" alertas={alertas} onReintentar={() => {}} />
    )
    expect(screen.getByText('Sarampión')).toBeInTheDocument()
  })

  it('muestra el título del panel cuando no está en idle', () => {
    render(
      <PanelAlertas estado="con_alertas" alertas={alertas} onReintentar={() => {}} />
    )
    expect(screen.getByText(/Alertas Epidemiológicas/i)).toBeInTheDocument()
  })
})
