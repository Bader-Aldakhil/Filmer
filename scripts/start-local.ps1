Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host 'Starting Docker Desktop (if installed)...'
$dockerDesktopExe = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
if (Test-Path $dockerDesktopExe) {
    Start-Process $dockerDesktopExe | Out-Null
}

Write-Host 'Waiting for Docker engine...'
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        docker info | Out-Null
        $ready = $true
        break
    } catch {
        Start-Sleep -Milliseconds 2000
    }
}

if (-not $ready) {
    throw 'Docker engine is not ready. Start Docker Desktop manually and rerun this script.'
}

Write-Host 'Starting backend + database with Docker Compose...'
Push-Location $repoRoot
try {
    docker compose up -d postgres backend

    Write-Host 'Starting Angular dev server in a new PowerShell window...'
    Start-Process powershell -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$repoRoot\frontend'; npm start"
    )

    Write-Host 'Opening website...'
    Start-Process 'https://localhost:4200/'

    Write-Host 'Done. Frontend: https://localhost:4200  Backend: https://localhost:8443/api/v1/health'
} finally {
    Pop-Location
}
