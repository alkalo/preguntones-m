param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$api = Join-Path $lib 'paper-api.jar'
if (-not (Test-Path $api)) {
    $origen = Join-Path $root '..\viajes\lib\paper-api.jar'
    if (Test-Path $origen) {
        Copy-Item $origen $api
    } else {
        $url = 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-20260511.115010-91.jar'
        & curl.exe -fL -o $api $url
    }
    if (-not (Test-Path $api)) { throw 'No pude bajar paper-api' }
}

$papi = Join-Path $lib 'placeholderapi.jar'
if (-not (Test-Path $papi)) {
    $origen = Join-Path $root '..\lib-compile\placeholderapi.jar'
    if (Test-Path $origen) {
        Copy-Item $origen $papi
    }
    if (-not (Test-Path $papi)) { throw 'No está placeholderapi.jar' }
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$vault = Join-Path $lib 'vault-api.jar'
if (-not (Test-Path $vault)) {
    $origen = Join-Path $root '..\lib-compile\Vault.jar'
    if (Test-Path $origen) {
        Copy-Item $origen $vault
    } else {
        $otro = Join-Path $root '..\xo\lib\vault-api.jar'
        if (Test-Path $otro) { Copy-Item $otro $vault }
    }
}
if (-not (Test-Path $vault)) { throw 'No está Vault' }

$jars = @(Get-ChildItem (Join-Path $root '..\viajes\lib') -Filter *.jar | ForEach-Object { $_.FullName })
if ($jars.Count -eq 0) { $jars = @($api) }
$jars += $papi
$jars += $vault
$cp = $jars -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
$adventure = Join-Path $root '..\censura\lib\adventure-api.jar'
$examination = Join-Path $root '..\censura\lib\examination-api.jar'
if (-not (Test-Path $adventure)) { throw 'Falta adventure-api para las pruebas de Karma' }
if (-not (Test-Path $examination)) { throw 'Falta examination-api para las pruebas de Karma' }
$cpPruebas = "$adventure;$examination"
& javac --release 21 -encoding UTF-8 -proc:none -cp $cpPruebas -d $pruebas `
    (Join-Path $root 'src\com\preguntones\karma\Etiqueta.java') `
    (Join-Path $root 'src\com\preguntones\karma\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$pruebas;$cpPruebas" com.preguntones.karma.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Karma pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
$jar = Join-Path $root 'Karma.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Karma.jar') -Force
Write-Host "Karma.jar listo"
