# Ingesta de Datos

Scripts para generar y publicar senales de outbreaks en el backend de StatusScope.

Esta carpeta vive en `tools/ingesta-datos` para que Quarkus no la compile ni la empaquete. Los unicos archivos versionables que consume la aplicacion quedan en `src/main/resources/data/outbreaks`.

## Comando Principal

Desde la raiz del backend:

```powershell
.\tools\ingesta-datos\actualizar_outbreaks.ps1
```

El script ejecuta:

1. Descarga y procesamiento municipal.
2. Descarga/deteccion del boletin epidemiologico estatal.
3. Extraccion y filtrado estatal.
4. Publicacion de `municipal_outbreaks.csv` y `state_outbreaks.csv` al backend.

Opciones utiles:

```powershell
.\tools\ingesta-datos\actualizar_outbreaks.ps1 --force-state-check
.\tools\ingesta-datos\actualizar_outbreaks.ps1 --skip-municipal
.\tools\ingesta-datos\actualizar_outbreaks.ps1 --skip-state
.\tools\ingesta-datos\actualizar_outbreaks.ps1 --no-publish-backend
```

`--force-state-check` fuerza una nueva busqueda del boletin, aunque ya se haya revisado en las ultimas 12 horas.

## Estructura

```text
tools/ingesta-datos/
  actualizar_outbreaks.ps1
  requirements.txt
  programas/
    ejecutar_ingesta_outbreaks.py
    comun/
      outbreaks.py
      procesos.py
      rutas.py
      xlsx.py
    municipal/
      ejecutar_ingesta_municipal.py
      descargar_datos_abiertos.py
      extraer_respiratorias.py
      extraer_febriles_exantematicas.py
      extraer_dengue.py
      combinar_outbreaks_municipales.py
      actualizar_coordenadas_inegi.py
    estatal/
      ejecutar_ingesta_estatal.py
      descargar_boletin_semanal.py
      extraer_pdf_boletin.py
      filtrar_enfermedades_relevantes.py
```

`comun` contiene rutas, utilidades de fechas/normalizacion, lectura XLSX y helpers para ejecutar modulos o publicar CSVs. `municipal` y `estatal` agrupan los procesos especificos de cada nivel.

## Modulos Python

Desde `tools/ingesta-datos/programas` tambien se pueden ejecutar modulos puntuales:

```powershell
python -m municipal.ejecutar_ingesta_municipal --help
python -m estatal.ejecutar_ingesta_estatal --help
```

No se conservan wrappers con los nombres antiguos. Equivalencias principales:

| Antes | Ahora |
| --- | --- |
| `run_outbreak_ingestion.py` | `ejecutar_ingesta_outbreaks.py` |
| `run_municipal_outbreak_ingestion.py` | `municipal/ejecutar_ingesta_municipal.py` |
| `download_open_data_sources.py` | `municipal/descargar_datos_abiertos.py` |
| `extract_respiratory_municipal.py` | `municipal/extraer_respiratorias.py` |
| `extract_febrile_exanthematous_municipal.py` | `municipal/extraer_febriles_exantematicas.py` |
| `extract_dengue_municipal.py` | `municipal/extraer_dengue.py` |
| `combine_municipal_outbreaks.py` | `municipal/combinar_outbreaks_municipales.py` |
| `update_municipality_coordinates_from_inegi.py` | `municipal/actualizar_coordenadas_inegi.py` |
| `run_state_outbreak_ingestion.py` | `estatal/ejecutar_ingesta_estatal.py` |
| `download_weekly_bulletin.py` | `estatal/descargar_boletin_semanal.py` |
| `extract_state_pdf.py` | `estatal/extraer_pdf_boletin.py` |
| `filter_state_outbreak_relevant.py` | `estatal/filtrar_enfermedades_relevantes.py` |

## Dependencias Python

El extractor estatal usa `pypdf`. Para instalarlo localmente:

```powershell
python -m pip install -r .\tools\ingesta-datos\requirements.txt
```

GitHub Actions instala esta dependencia automaticamente.

## Datos De Trabajo

Los ZIPs, PDFs y CSVs intermedios se guardan en:

```text
tools/ingesta-datos/.data/
```

Esa carpeta esta ignorada por Git. Los CSV finales versionables son:

```text
src/main/resources/data/outbreaks/municipal_outbreaks.csv
src/main/resources/data/outbreaks/state_outbreaks.csv
```

El backend importa estos CSV al arrancar cuando `OUTBREAK_INGESTION_IMPORT_AT_START=true`, que es el valor local por defecto en `application.properties`.

## GitHub Actions

El workflow `StatusScope-Backend/.github/workflows/update-outbreaks.yml` en este workspace ejecuta la ingesta cada jueves a las 14:00 UTC, equivalente a las 08:00 en Mexico City, y tambien permite ejecucion manual desde `workflow_dispatch`. Si el backend se maneja como repositorio independiente, esa misma ruta aparece como `.github/workflows/update-outbreaks.yml` dentro del repo del backend.

Si los CSV cambian, el workflow hace commit automatico con el usuario `github-actions[bot]`.

Nota: gob.mx puede bloquear la descarga directa del PDF estatal. Si eso pasa, el pipeline conserva el `state_outbreaks.csv` ya versionado y deja el aviso en logs; la ingesta municipal puede seguir actualizandose.
