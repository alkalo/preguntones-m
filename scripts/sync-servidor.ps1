$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
& node (Join-Path $PSScriptRoot 'sync-servidor.mjs') @args
exit $LASTEXITCODE
