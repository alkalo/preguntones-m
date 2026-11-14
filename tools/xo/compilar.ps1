param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$vault = Join-Path $lib 'vault-api.jar'
if (-not (Test-Path $vault) -or (Get-Item $vault).Length -lt 1000) {
    curl.exe -fL -o $vault 'https://jitpack.io/com/github/MilkBowl/VaultAPI/1.7.1/VaultAPI-1.7.1.jar'
}
$deServidor = Join-Path $root '..\..\servidor\plugins\Vault.jar'
if (Test-Path $deServidor) {
    $vault = $deServidor
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$viajesLib = Join-Path $root '..\viajes\lib'
$jars = @(Get-ChildItem $viajesLib -Filter *.jar | ForEach-Object { $_.FullName })
$jars += $vault
$cp = $jars -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -d $pruebas `
    (Join-Path $root 'src\com\preguntones\xo\Cita.java') `
    (Join-Path $root 'src\com\preguntones\xo\Bote.java') `
    (Join-Path $root 'src\com\preguntones\xo\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.xo.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Xo pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
Copy-Item (Join-Path $root 'preguntas.yml') $build
$jar = Join-Path $root 'Xo.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Xo.jar') -Force
Write-Host 'Xo.jar listo'
