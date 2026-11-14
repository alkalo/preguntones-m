$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'sync-servidor.ps1') copia
exit $LASTEXITCODE
