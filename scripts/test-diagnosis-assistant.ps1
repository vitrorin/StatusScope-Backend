param(
    [string]$MavenRepoLocal
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

function Resolve-MavenCommand {
    $cachedMaven = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd"
    if (Test-Path $cachedMaven) {
        return $cachedMaven
    }

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) {
        return $mvn.Source
    }

    $wrapper = Join-Path $projectRoot "mvnw.cmd"
    if (Test-Path $wrapper) {
        return $wrapper
    }

    throw "Could not locate Maven. Install Maven, restore the wrapper, or populate the cached Maven distribution."
}

$mavenCmd = Resolve-MavenCommand
$args = @(
    "-Dquarkus.profile=test",
    "-Dquarkus.datasource.db-kind=h2",
    "-Dquarkus.datasource.jdbc.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "-Dquarkus.datasource.username=sa",
    "-Dquarkus.datasource.password=",
    "-Dtest=AssistantPromptBuilderTest,AskDiagnosisAssistantUseCaseTest,GetDiagnosisAssistantThreadUseCaseTest,LlmChatClientTest,DiagnosisAssistantResourceTest,DiagnosisEvaluationResourceTest",
    "test"
)

if ($MavenRepoLocal) {
    $args = @("-Dmaven.repo.local=$MavenRepoLocal") + $args
}

Write-Host "Running diagnosis assistant tests with: $mavenCmd"
Push-Location $projectRoot
try {
    & $mavenCmd @args
}
finally {
    Pop-Location
}
