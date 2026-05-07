param(
    [string]$TaskName = "StatusScope - Actualizar outbreaks",
    [ValidateSet("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")]
    [string]$DayOfWeek = "Thursday",
    [string]$At = "08:00",
    [switch]$NoForceStateCheck,
    [switch]$Remove
)

$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsRoot = Split-Path -Parent $ScriptRoot
$BackendRoot = Split-Path -Parent $ToolsRoot
$PipelineScript = Join-Path $ScriptRoot "actualizar_outbreaks.ps1"

if ($Remove) {
    if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
        Write-Host "Tarea eliminada: $TaskName"
    } else {
        Write-Host "No existe la tarea: $TaskName"
    }
    exit 0
}

if (-not (Test-Path -LiteralPath $PipelineScript)) {
    throw "No se encontro el script de ingesta: $PipelineScript"
}

$time = [datetime]::ParseExact($At, "HH:mm", $null)
$pipelineArgs = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", "`"$PipelineScript`""
)

if (-not $NoForceStateCheck) {
    $pipelineArgs += "--force-state-check"
}

$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument ($pipelineArgs -join " ") `
    -WorkingDirectory $BackendRoot

$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $DayOfWeek -At $time
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries

$description = "Actualiza outbreaks municipales y estatales desde fuentes oficiales de Salud y publica CSVs al backend."

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Description $description `
    -Force | Out-Null

Write-Host "Tarea instalada: $TaskName"
Write-Host "Frecuencia: cada $DayOfWeek a las $At"
Write-Host "Comando: powershell.exe $($pipelineArgs -join ' ')"
