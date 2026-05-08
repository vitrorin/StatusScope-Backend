# Ingesta de Datos

Scripts para generar y publicar senales de outbreaks en el backend de StatusScope.

Esta carpeta vive en `tools/ingesta-datos` para que Quarkus no la compile ni la empaquete. Los unicos archivos que consume la aplicacion quedan en `src/main/resources/data/outbreaks`.

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

## GitHub Actions

El workflow `.github/workflows/update-outbreaks.yml` ejecuta la ingesta cada jueves a las 14:00 UTC, equivalente a las 08:00 en Mexico City, y tambien permite ejecucion manual desde `workflow_dispatch`.

Si los CSV cambian, el workflow hace commit automatico con el usuario `github-actions[bot]`.

Nota: gob.mx puede bloquear la descarga directa del PDF estatal. Si eso pasa, el pipeline conserva el `state_outbreaks.csv` ya versionado y deja el aviso en logs; la ingesta municipal puede seguir actualizandose.
