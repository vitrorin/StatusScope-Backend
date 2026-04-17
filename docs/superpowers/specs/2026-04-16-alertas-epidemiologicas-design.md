# Diseño: Componente de Alertas Epidemiológicas

**Fecha:** 2026-04-16
**Historia de usuario:** HU-03 — Alertas epidemiológicas durante el diagnóstico
**EPIC:** EP-02 — Alertas Epidemiológicas
**Responsable:** Victor D. Bosquez González (A01660465)
**Stack:** React + Tailwind CSS

---

## Resumen

Componente sidebar derecho que aparece en la pantalla del Asistente Diagnóstico después de que el doctor presiona "Analizar". Muestra alertas epidemiológicas de la zona geográfica del paciente que coincidan con los síntomas ingresados. Maneja 4 estados: cargando, con alertas, sin alertas y error.

---

## Árbol de componentes

```
<AsistenteDiagnostico>
  ├── <FormularioSintomas>
  ├── <ResultadosDiagnostico>
  └── <PanelAlertas>
        ├── <CabeceraPanelAlertas>
        ├── <EstadoCargando>
        ├── <EstadoSinAlertas>
        ├── <EstadoError>
        └── <ListaAlertas>
              └── <TarjetaAlerta> × n
                    ├── BadgeSeveridad
                    ├── DatosCasos
                    ├── PruebasSugeridas
                    └── EnlacePerfilEpidemiologico
```

---

## Estados del componente

| Estado | Cuándo ocurre | UI |
|---|---|---|
| `idle` | Antes de presionar "Analizar" | Panel no visible |
| `cargando` | Fetch en curso | Skeleton loader con texto "Analizando contexto epidemiológico..." |
| `con_alertas` | API devuelve ≥1 alerta | Lista de `<TarjetaAlerta>` |
| `sin_alertas` | API devuelve lista vacía | Ícono ✓ verde + "Sin alertas activas" |
| `error` | Fallo de red o error del servidor | Mensaje de error + botón "Reintentar" |

---

## Flujo de datos

1. `<AsistenteDiagnostico>` es dueño del estado: `{ estadoAlertas, alertas }`.
2. Al presionar "Analizar": el padre hace el fetch y pone `estadoAlertas = 'cargando'`.
3. Al resolver: `estadoAlertas = 'con_alertas' | 'sin_alertas' | 'error'`.
4. `<PanelAlertas>` recibe `{ estado, alertas }` como props — no hace fetch propio.

---

## Interfaz de datos

### Props de `<PanelAlertas>`

```ts
interface PanelAlertasProps {
  estado: 'idle' | 'cargando' | 'con_alertas' | 'sin_alertas' | 'error'
  alertas: Alerta[]
  onReintentar: () => void
}
```

### Tipo `Alerta` (respuesta del API)

```ts
interface Alerta {
  id: string
  enfermedad: string
  severidad: 'alto' | 'medio' | 'bajo'
  codigoPostal: string
  casosConfirmados: number
  tendenciaSemanal: number   // positivo = aumento, negativo = descenso
  pruebasSugeridas: string[]
  perfilUrl: string
}
```

---

## Diseño visual de `<TarjetaAlerta>`

Cada tarjeta muestra:
- **Nombre de la enfermedad** + **BadgeSeveridad** (ALTO = rojo, MEDIO = amarillo, BAJO = azul)
- **CP** + número de casos confirmados
- **Tendencia semanal** (↑ +N / ↓ -N vs semana anterior)
- **Pruebas sugeridas** (lista simple)
- **Enlace** "Ver perfil epidemiológico →"

El color del borde izquierdo de la tarjeta refleja la severidad:
- Alto → `border-red-500`
- Medio → `border-yellow-500`
- Bajo → `border-blue-400`

> **Nota:** El diseño visual exacto (colores, espaciado, tipografía) es flexible y puede iterarse sin cambiar la arquitectura.

---

## Criterios de aceptación (HU-03)

- [x] El sistema analiza enfermedades activas en la zona del paciente (por CP)
- [x] El sistema muestra alertas cuando los síntomas coinciden con enfermedades detectadas
- [x] Las alertas incluyen información epidemiológica relevante (casos, tendencia, pruebas sugeridas)
- [x] El componente comunica claramente el estado de carga y ausencia de alertas

---

## Fuera de alcance (este componente)

- Mapa geográfico de distribución de casos (pertenece al radar del administrador, EP-04)
- Historial de alertas previas del doctor
- Notificaciones push o alertas fuera del flujo de diagnóstico
