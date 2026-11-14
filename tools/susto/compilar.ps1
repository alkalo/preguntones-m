param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root '..\viajes\lib'
$jars = @(Get-ChildItem $lib -Filter *.jar | ForEach-Object { $_.FullName })
if ($jars.Count -eq 0) { throw 'No está paper-api' }
$cp = $jars -join ';'

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -d $pruebas `
    (Join-Path $root 'src\com\preguntones\susto\Caras.java') `
    (Join-Path $root 'src\com\preguntones\susto\Ritmo.java') `
    (Join-Path $root 'src\com\preguntones\susto\Elegir.java') `
    (Join-Path $root 'src\com\preguntones\susto\Gritos.java') `
    (Join-Path $root 'src\com\preguntones\susto\Pantalla.java') `
    (Join-Path $root 'src\com\preguntones\susto\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.susto.Pruebas $pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Susto pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
$jar = Join-Path $root 'Susto.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Susto.jar') -Force
Write-Host 'Susto.jar listo'
