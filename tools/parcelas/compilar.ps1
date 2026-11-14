param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$paper = Join-Path $lib 'paper-api.jar'
if (-not (Test-Path $paper)) {
    Copy-Item (Join-Path $root '..\viajes\lib\paper-api.jar') $paper
}
$vault = Join-Path $lib 'Vault.jar'
if (-not (Test-Path $vault) -or (Get-Item $vault).Length -lt 1000) {
    $deServidor = Join-Path $root '..\..\servidor\plugins\Vault.jar'
    if (Test-Path $deServidor) {
        Copy-Item $deServidor $vault
    } else {
        foreach ($origen in @(
            (Join-Path $root '..\lib-compile\Vault.jar'),
            (Join-Path $root '..\xo\lib\vault-api.jar'),
            (Join-Path $root '..\bingo\lib\vault-api.jar')
        )) {
            if (Test-Path $origen) {
                Copy-Item $origen $vault -Force
                break
            }
        }
        if (-not (Test-Path $vault)) {
            & curl.exe -fL -o $vault 'https://jitpack.io/com/github/MilkBowl/VaultAPI/1.7.1/VaultAPI-1.7.1.jar'
        }
        if (-not (Test-Path $vault)) { throw 'Falta Vault.jar en tools/parcelas/lib' }
    }
}
$placeholders = Join-Path $lib 'PlaceholderAPI.jar'
if (-not (Test-Path $placeholders)) {
    foreach ($origen in @(
        (Join-Path $root '..\lib-compile\placeholderapi.jar'),
        (Join-Path $root '..\..\servidor\plugins\PlaceholderAPI-2.12.3.jar')
    )) {
        if (Test-Path $origen) {
            Copy-Item $origen $placeholders -Force
            break
        }
    }
    if (-not (Test-Path $placeholders)) {
        & curl.exe -fL -o $placeholders 'https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.12.3/PlaceholderAPI-2.12.3.jar'
    }
    if (-not (Test-Path $placeholders)) { throw 'Falta PlaceholderAPI.jar en tools/parcelas/lib' }
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$extra = Get-ChildItem (Join-Path $root '..\viajes\lib') -Filter *.jar | ForEach-Object { $_.FullName }
$cp = (@($paper, $vault, $placeholders) + $extra) -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -d $pruebas (Join-Path $root 'src\com\preguntones\parcelas\Cuadricula.java') (Join-Path $root 'src\com\preguntones\parcelas\Camino.java') (Join-Path $root 'src\com\preguntones\parcelas\Cuentas.java') (Join-Path $root 'src\com\preguntones\parcelas\Reparto.java') (Join-Path $root 'src\com\preguntones\parcelas\Entrada.java') (Join-Path $root 'src\com\preguntones\parcelas\Cupo.java') (Join-Path $root 'src\com\preguntones\parcelas\Enlace.java') (Join-Path $root 'src\com\preguntones\parcelas\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.parcelas.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Parcelas pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
$jar = Join-Path $root 'Parcelas.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Parcelas.jar') -Force
Write-Host 'Parcelas.jar listo'
