Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host 'Stopping Docker Compose services...'
Push-Location $repoRoot
try {
    docker compose down
} finally {
    Pop-Location
}

Write-Host 'Done. Note: close any Angular terminal window manually if it is still running.'
