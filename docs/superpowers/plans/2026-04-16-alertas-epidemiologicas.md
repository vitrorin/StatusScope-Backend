# Componente de Alertas Epidemiológicas — Plan de Implementación

> **Para agentes:** SUB-SKILL REQUERIDO: Usar `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans` para ejecutar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Objetivo:** Implementar el componente `<PanelAlertas>` y sus sub-componentes en React + Tailwind, cubriendo los 4 estados (cargando, con_alertas, sin_alertas, error) de la HU-03.

**Arquitectura:** `<PanelAlertas>` es un sidebar derecho controlado por props — no hace fetch propio. El componente padre (`<AsistenteDiagnostico>`) maneja el estado y lo pasa como `{ estado, alertas, onReintentar }`. Cada estado renderiza un sub-componente distinto.

**Tech Stack:** React 18, TypeScript, Tailwind CSS, Vite, Vitest, React Testing Library

---

## Estructura de archivos

```
frontend/
  src/
    types/
      alerta.ts                          ← Interfaces Alerta y PanelAlertasProps
    components/
      alertas/
        BadgeSeveridad.tsx               ← Badge ALTO/MEDIO/BAJO
        EstadoCargando.tsx               ← Skeleton loader
        EstadoSinAlertas.tsx             ← Estado vacío
        EstadoError.tsx                  ← Estado de error + botón reintentar
        TarjetaAlerta.tsx                ← Tarjeta individual de alerta
        ListaAlertas.tsx                 ← Contenedor de lista de tarjetas
        PanelAlertas.tsx                 ← Componente principal (sidebar)
        index.ts                         ← Re-exportaciones públicas
        __tests__/
          BadgeSeveridad.test.tsx
          EstadoCargando.test.tsx
          EstadoSinAlertas.test.tsx
          EstadoError.test.tsx
          TarjetaAlerta.test.tsx
          ListaAlertas.test.tsx
          PanelAlertas.test.tsx
    test/
      setup.ts                           ← Configuración global de jest-dom
  vite.config.ts
  tailwind.config.js
  index.html
  package.json
```

---

## Tarea 1: Configurar proyecto React + Vite + Tailwind + Vitest

**Archivos:**
- Crear: `frontend/` (directorio raíz del proyecto)
- Crear: `frontend/vite.config.ts`
- Crear: `frontend/tailwind.config.js`
- Crear: `frontend/src/test/setup.ts`

- [ ] **Paso 1: Crear proyecto Vite con plantilla React + TypeScript**

Ejecutar desde la raíz del repo:
```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
```

- [ ] **Paso 2: Instalar Tailwind CSS**

```bash
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

- [ ] **Paso 3: Instalar dependencias de testing**

```bash
npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom
```

- [ ] **Paso 4: Configurar Vitest en `frontend/vite.config.ts`**

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
  },
})
```

- [ ] **Paso 5: Crear archivo de setup de tests `frontend/src/test/setup.ts`**

```ts
import '@testing-library/jest-dom'
```

- [ ] **Paso 6: Configurar Tailwind en `frontend/tailwind.config.js`**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: { extend: {} },
  plugins: [],
}
```

- [ ] **Paso 7: Agregar directivas Tailwind a `frontend/src/index.css`**

Reemplazar el contenido de `src/index.css` con:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Paso 8: Verificar que el proyecto corre**

```bash
npm run dev
```
Esperado: servidor en `http://localhost:5173` sin errores.

- [ ] **Paso 9: Verificar que los tests corren**

```bash
npm run test
```
Esperado: `No test files found` (no hay tests aún, pero Vitest arranca sin error).

- [ ] **Paso 10: Commit**

```bash
cd ..
git add frontend/
git commit -m "feat: inicializar proyecto React + Vite + Tailwind + Vitest"
```

---

## Tarea 2: Definir tipos TypeScript

**Archivos:**
- Crear: `frontend/src/types/alerta.ts`

- [ ] **Paso 1: Crear `frontend/src/types/alerta.ts`**

```ts
export interface Alerta {
  id: string
  enfermedad: string
  severidad: 'alto' | 'medio' | 'bajo'
  codigoPostal: string
  casosConfirmados: number
  tendenciaSemanal: number
  pruebasSugeridas: string[]
  perfilUrl: string
}

export interface PanelAlertasProps {
  estado: 'idle' | 'cargando' | 'con_alertas' | 'sin_alertas' | 'error'
  alertas: Alerta[]
  onReintentar: () => void
}
```

- [ ] **Paso 2: Verificar que TypeScript compila**

```bash
cd frontend && npx tsc --noEmit
```
Esperado: sin errores.

- [ ] **Paso 3: Commit**

```bash
cd ..
git add frontend/src/types/alerta.ts
git commit -m "feat: definir tipos Alerta y PanelAlertasProps"
```

---

## Tarea 3: Componente `BadgeSeveridad`

**Archivos:**
- Crear: `frontend/src/components/alertas/BadgeSeveridad.tsx`
- Crear: `frontend/src/components/alertas/__tests__/BadgeSeveridad.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/BadgeSeveridad.test.tsx`:

```tsx
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
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/BadgeSeveridad.test.tsx
```
Esperado: FAIL — `Cannot find module '../BadgeSeveridad'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/BadgeSeveridad.tsx`**

```tsx
interface BadgeSeveridadProps {
  severidad: 'alto' | 'medio' | 'bajo'
}

const estilos: Record<BadgeSeveridadProps['severidad'], string> = {
  alto: 'bg-red-500/10 text-red-400 border border-red-500',
  medio: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500',
  bajo: 'bg-blue-500/10 text-blue-400 border border-blue-400',
}

const etiquetas: Record<BadgeSeveridadProps['severidad'], string> = {
  alto: 'ALTO',
  medio: 'MEDIO',
  bajo: 'BAJO',
}

export function BadgeSeveridad({ severidad }: BadgeSeveridadProps) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${estilos[severidad]}`}>
      {etiquetas[severidad]}
    </span>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/BadgeSeveridad.test.tsx
```
Esperado: 6 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/BadgeSeveridad.tsx frontend/src/components/alertas/__tests__/BadgeSeveridad.test.tsx
git commit -m "feat: agregar componente BadgeSeveridad con tests"
```

---

## Tarea 4: Componente `EstadoCargando`

**Archivos:**
- Crear: `frontend/src/components/alertas/EstadoCargando.tsx`
- Crear: `frontend/src/components/alertas/__tests__/EstadoCargando.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/EstadoCargando.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { EstadoCargando } from '../EstadoCargando'

describe('EstadoCargando', () => {
  it('muestra el mensaje de análisis en curso', () => {
    render(<EstadoCargando />)
    expect(screen.getByText('Analizando contexto epidemiológico...')).toBeInTheDocument()
  })

  it('renderiza elementos skeleton', () => {
    const { container } = render(<EstadoCargando />)
    const skeletons = container.querySelectorAll('.animate-pulse')
    expect(skeletons.length).toBeGreaterThanOrEqual(2)
  })
})
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/EstadoCargando.test.tsx
```
Esperado: FAIL — `Cannot find module '../EstadoCargando'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/EstadoCargando.tsx`**

```tsx
export function EstadoCargando() {
  return (
    <div className="flex flex-col gap-3">
      <div className="animate-pulse bg-purple-900/40 rounded-lg p-3">
        <div className="h-3 bg-purple-800/60 rounded w-3/5 mb-2" />
        <div className="h-2 bg-purple-900/60 rounded w-4/5 mb-1" />
        <div className="h-2 bg-purple-900/60 rounded w-1/2" />
      </div>
      <div className="animate-pulse bg-blue-900/20 rounded-lg p-3">
        <div className="h-3 bg-blue-800/40 rounded w-1/2 mb-2" />
        <div className="h-2 bg-blue-900/30 rounded w-3/4 mb-1" />
        <div className="h-2 bg-blue-900/30 rounded w-2/5" />
      </div>
      <p className="text-center text-purple-600 text-xs mt-1">
        Analizando contexto epidemiológico...
      </p>
    </div>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/EstadoCargando.test.tsx
```
Esperado: 2 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/EstadoCargando.tsx frontend/src/components/alertas/__tests__/EstadoCargando.test.tsx
git commit -m "feat: agregar componente EstadoCargando con tests"
```

---

## Tarea 5: Componente `EstadoSinAlertas`

**Archivos:**
- Crear: `frontend/src/components/alertas/EstadoSinAlertas.tsx`
- Crear: `frontend/src/components/alertas/__tests__/EstadoSinAlertas.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/EstadoSinAlertas.test.tsx`:

```tsx
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
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/EstadoSinAlertas.test.tsx
```
Esperado: FAIL — `Cannot find module '../EstadoSinAlertas'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/EstadoSinAlertas.tsx`**

```tsx
export function EstadoSinAlertas() {
  return (
    <div className="flex flex-col items-center justify-center py-8 px-3 text-center">
      <span className="text-3xl mb-3" aria-hidden="true">✓</span>
      <p className="text-green-400 font-semibold text-sm mb-2">Sin alertas activas</p>
      <p className="text-slate-500 text-xs leading-relaxed">
        No se detectaron brotes en la zona que coincidan con los síntomas registrados.
      </p>
    </div>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/EstadoSinAlertas.test.tsx
```
Esperado: 2 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/EstadoSinAlertas.tsx frontend/src/components/alertas/__tests__/EstadoSinAlertas.test.tsx
git commit -m "feat: agregar componente EstadoSinAlertas con tests"
```

---

## Tarea 6: Componente `EstadoError`

**Archivos:**
- Crear: `frontend/src/components/alertas/EstadoError.tsx`
- Crear: `frontend/src/components/alertas/__tests__/EstadoError.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/EstadoError.test.tsx`:

```tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { EstadoError } from '../EstadoError'

describe('EstadoError', () => {
  it('muestra el título de error', () => {
    render(<EstadoError onReintentar={() => {}} />)
    expect(screen.getByText('No se pudo cargar')).toBeInTheDocument()
  })

  it('muestra descripción del error', () => {
    render(<EstadoError onReintentar={() => {}} />)
    expect(
      screen.getByText(/Error al consultar el contexto epidemiológico/i)
    ).toBeInTheDocument()
  })

  it('muestra el botón Reintentar', () => {
    render(<EstadoError onReintentar={() => {}} />)
    expect(screen.getByRole('button', { name: /reintentar/i })).toBeInTheDocument()
  })

  it('llama onReintentar al hacer clic en el botón', () => {
    const onReintentar = vi.fn()
    render(<EstadoError onReintentar={onReintentar} />)
    fireEvent.click(screen.getByRole('button', { name: /reintentar/i }))
    expect(onReintentar).toHaveBeenCalledTimes(1)
  })
})
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/EstadoError.test.tsx
```
Esperado: FAIL — `Cannot find module '../EstadoError'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/EstadoError.tsx`**

```tsx
interface EstadoErrorProps {
  onReintentar: () => void
}

export function EstadoError({ onReintentar }: EstadoErrorProps) {
  return (
    <div className="flex flex-col items-center justify-center py-6 px-3 text-center">
      <span className="text-2xl mb-3" aria-hidden="true">⚠️</span>
      <p className="text-red-400 font-semibold text-sm mb-2">No se pudo cargar</p>
      <p className="text-slate-500 text-xs leading-relaxed mb-4">
        Error al consultar el contexto epidemiológico.
      </p>
      <button
        onClick={onReintentar}
        className="bg-purple-900/30 text-purple-400 border border-purple-700 rounded-md px-4 py-1.5 text-xs hover:bg-purple-900/50 transition-colors"
      >
        Reintentar
      </button>
    </div>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/EstadoError.test.tsx
```
Esperado: 4 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/EstadoError.tsx frontend/src/components/alertas/__tests__/EstadoError.test.tsx
git commit -m "feat: agregar componente EstadoError con tests"
```

---

## Tarea 7: Componente `TarjetaAlerta`

**Archivos:**
- Crear: `frontend/src/components/alertas/TarjetaAlerta.tsx`
- Crear: `frontend/src/components/alertas/__tests__/TarjetaAlerta.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/TarjetaAlerta.test.tsx`:

```tsx
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
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/TarjetaAlerta.test.tsx
```
Esperado: FAIL — `Cannot find module '../TarjetaAlerta'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/TarjetaAlerta.tsx`**

```tsx
import { Alerta } from '../../types/alerta'
import { BadgeSeveridad } from './BadgeSeveridad'

interface TarjetaAlertaProps {
  alerta: Alerta
}

const borderColor: Record<Alerta['severidad'], string> = {
  alto: 'border-l-red-500',
  medio: 'border-l-yellow-500',
  bajo: 'border-l-blue-400',
}

const bgColor: Record<Alerta['severidad'], string> = {
  alto: 'bg-purple-900/30',
  medio: 'bg-blue-900/20',
  bajo: 'bg-slate-800/30',
}

const nombreColor: Record<Alerta['severidad'], string> = {
  alto: 'text-yellow-400',
  medio: 'text-blue-400',
  bajo: 'text-slate-300',
}

export function TarjetaAlerta({ alerta }: TarjetaAlertaProps) {
  const tendenciaTexto =
    alerta.tendenciaSemanal >= 0
      ? `↑ +${alerta.tendenciaSemanal} vs semana anterior`
      : `↓ ${alerta.tendenciaSemanal} vs semana anterior`

  return (
    <div
      className={`${bgColor[alerta.severidad]} border-l-4 ${borderColor[alerta.severidad]} rounded-md p-3`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className={`font-bold text-sm ${nombreColor[alerta.severidad]}`}>
          {alerta.enfermedad}
        </span>
        <BadgeSeveridad severidad={alerta.severidad} />
      </div>

      <p className="text-slate-300 text-xs mb-1">
        📍 CP {alerta.codigoPostal} · {alerta.casosConfirmados} casos confirmados
      </p>
      <p className="text-green-400 text-xs mb-3">{tendenciaTexto}</p>

      <div className="border-t border-purple-800/30 pt-2 mb-2">
        <p className="text-slate-500 text-xs uppercase tracking-wide mb-1">
          Pruebas sugeridas
        </p>
        <p className="text-slate-300 text-xs">
          {alerta.pruebasSugeridas.map((p) => `• ${p}`).join('  ')}
        </p>
      </div>

      <div className="text-right">
        <a
          href={alerta.perfilUrl}
          className="text-purple-400 text-xs underline hover:text-purple-300"
        >
          Ver perfil epidemiológico →
        </a>
      </div>
    </div>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/TarjetaAlerta.test.tsx
```
Esperado: 9 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/TarjetaAlerta.tsx frontend/src/components/alertas/__tests__/TarjetaAlerta.test.tsx
git commit -m "feat: agregar componente TarjetaAlerta con tests"
```

---

## Tarea 8: Componente `ListaAlertas`

**Archivos:**
- Crear: `frontend/src/components/alertas/ListaAlertas.tsx`
- Crear: `frontend/src/components/alertas/__tests__/ListaAlertas.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/ListaAlertas.test.tsx`:

```tsx
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
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/ListaAlertas.test.tsx
```
Esperado: FAIL — `Cannot find module '../ListaAlertas'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/ListaAlertas.tsx`**

```tsx
import { Alerta } from '../../types/alerta'
import { TarjetaAlerta } from './TarjetaAlerta'

interface ListaAlertasProps {
  alertas: Alerta[]
}

export function ListaAlertas({ alertas }: ListaAlertasProps) {
  return (
    <div className="flex flex-col gap-3">
      {alertas.map((alerta) => (
        <TarjetaAlerta key={alerta.id} alerta={alerta} />
      ))}
    </div>
  )
}
```

- [ ] **Paso 4: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/ListaAlertas.test.tsx
```
Esperado: 2 tests PASS

- [ ] **Paso 5: Commit**

```bash
cd ..
git add frontend/src/components/alertas/ListaAlertas.tsx frontend/src/components/alertas/__tests__/ListaAlertas.test.tsx
git commit -m "feat: agregar componente ListaAlertas con tests"
```

---

## Tarea 9: Componente principal `PanelAlertas`

**Archivos:**
- Crear: `frontend/src/components/alertas/PanelAlertas.tsx`
- Crear: `frontend/src/components/alertas/index.ts`
- Crear: `frontend/src/components/alertas/__tests__/PanelAlertas.test.tsx`

- [ ] **Paso 1: Escribir el test que falla**

Crear `frontend/src/components/alertas/__tests__/PanelAlertas.test.tsx`:

```tsx
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
```

- [ ] **Paso 2: Correr el test para confirmar que falla**

```bash
cd frontend && npx vitest run src/components/alertas/__tests__/PanelAlertas.test.tsx
```
Esperado: FAIL — `Cannot find module '../PanelAlertas'`

- [ ] **Paso 3: Crear `frontend/src/components/alertas/PanelAlertas.tsx`**

```tsx
import { PanelAlertasProps } from '../../types/alerta'
import { EstadoCargando } from './EstadoCargando'
import { EstadoSinAlertas } from './EstadoSinAlertas'
import { EstadoError } from './EstadoError'
import { ListaAlertas } from './ListaAlertas'

export function PanelAlertas({ estado, alertas, onReintentar }: PanelAlertasProps) {
  if (estado === 'idle') return null

  return (
    <aside className="w-64 flex-shrink-0 bg-purple-950/60 border border-purple-800 rounded-xl p-4">
      <h2 className="text-purple-400 text-xs font-bold tracking-widest mb-4 uppercase">
        ⚠ Alertas Epidemiológicas
      </h2>
      {estado === 'cargando' && <EstadoCargando />}
      {estado === 'sin_alertas' && <EstadoSinAlertas />}
      {estado === 'error' && <EstadoError onReintentar={onReintentar} />}
      {estado === 'con_alertas' && <ListaAlertas alertas={alertas} />}
    </aside>
  )
}
```

- [ ] **Paso 4: Crear `frontend/src/components/alertas/index.ts`**

```ts
export { PanelAlertas } from './PanelAlertas'
export { TarjetaAlerta } from './TarjetaAlerta'
export { BadgeSeveridad } from './BadgeSeveridad'
export type { Alerta, PanelAlertasProps } from '../../types/alerta'
```

- [ ] **Paso 5: Correr el test para confirmar que pasa**

```bash
npx vitest run src/components/alertas/__tests__/PanelAlertas.test.tsx
```
Esperado: 7 tests PASS

- [ ] **Paso 6: Correr todos los tests para verificar que nada se rompió**

```bash
npx vitest run
```
Esperado: todos los tests PASS (sin errores ni regresiones)

- [ ] **Paso 7: Commit**

```bash
cd ..
git add frontend/src/components/alertas/PanelAlertas.tsx frontend/src/components/alertas/index.ts frontend/src/components/alertas/__tests__/PanelAlertas.test.tsx
git commit -m "feat: agregar componente PanelAlertas principal con tests (HU-03)"
```

---

## Resumen de cobertura

| Criterio de aceptación (HU-03) | Cubierto en |
|---|---|
| El sistema analiza enfermedades activas en la zona (por CP) | Tarea 7 — `TarjetaAlerta` muestra CP |
| Muestra alertas cuando síntomas coinciden con enfermedades detectadas | Tarea 9 — estado `con_alertas` |
| Las alertas incluyen información epidemiológica relevante | Tarea 7 — casos, tendencia, pruebas sugeridas |
| Comunica el estado de carga y ausencia de alertas | Tareas 4, 5, 6 — `EstadoCargando`, `EstadoSinAlertas`, `EstadoError` |
