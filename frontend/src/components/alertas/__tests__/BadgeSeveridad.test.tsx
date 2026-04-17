import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { BadgeSeveridad } from '../BadgeSeveridad'

describe('BadgeSeveridad', () => {
  it('muestra ALTO para severidad alto', () => {
    render(<BadgeSeveridad severidad="alto" />)
    expect(screen.getByText('ALTO')).toBeInTheDocument()
  })

  it('muestra MEDIO para severidad medio', () => {
    render(<BadgeSeveridad severidad="medio" />)
    expect(screen.getByText('MEDIO')).toBeInTheDocument()
  })

  it('muestra BAJO para severidad bajo', () => {
    render(<BadgeSeveridad severidad="bajo" />)
    expect(screen.getByText('BAJO')).toBeInTheDocument()
  })

  it('aplica clase de color rojo para severidad alto', () => {
    render(<BadgeSeveridad severidad="alto" />)
    expect(screen.getByText('ALTO')).toHaveClass('text-red-400')
  })

  it('aplica clase de color amarillo para severidad medio', () => {
    render(<BadgeSeveridad severidad="medio" />)
    expect(screen.getByText('MEDIO')).toHaveClass('text-yellow-400')
  })

  it('aplica clase de color azul para severidad bajo', () => {
    render(<BadgeSeveridad severidad="bajo" />)
    expect(screen.getByText('BAJO')).toHaveClass('text-blue-400')
  })
})
