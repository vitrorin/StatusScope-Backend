$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsRoot = Split-Path -Parent $ScriptRoot
$BackendRoot = Split-Path -Parent $ToolsRoot
$Pipeline = Join-Path $ScriptRoot "programas\ejecutar_ingesta_outbreaks.py"

Set-Location $BackendRoot
$env:PYTHONDONTWRITEBYTECODE = "1"

python $Pipeline @args
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
