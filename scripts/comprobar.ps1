$ErrorActionPreference = 'Stop'
& node (Join-Path $PSScriptRoot 'comprobar.mjs') @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
