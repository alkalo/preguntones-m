param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$vault = Join-Path $lib 'vault-api.jar'
if (-not (Test-Path $vault) -or (Get-Item $vault).Length -lt 1000) {
    $copias = @(
        (Join-Path $root '..\xo\lib\vault-api.jar'),
        (Join-Path $root '..\mundos\lib\vault-api.jar'),
        (Join-Path $root '..\lib-compile\Vault.jar'),
        (Join-Path $root '..\..\servidor\plugins\Vault.jar')
    )
    $puesto = $false
    foreach ($origen in $copias) {
        if (Test-Path $origen) {
            Copy-Item $origen $vault -Force
            $puesto = $true
            break
        }
    }
    if (-not $puesto) {
        curl.exe -fL -o $vault 'https://repo1.maven.org/maven2/com/github/MilkBowl/VaultAPI/1.7.1/VaultAPI-1.7.1.jar'
    }
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$jars = @(Get-ChildItem (Join-Path $root '..\viajes\lib') -Filter *.jar | ForEach-Object { $_.FullName })
if ($jars.Count -eq 0) { throw 'No está paper-api' }
$jars += $vault
$cp = $jars -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -d $pruebas `
    (Join-Path $root 'src\com\preguntones\bingo\Casilla.java') `
    (Join-Path $root 'src\com\preguntones\bingo\Carta.java') `
    (Join-Path $root 'src\com\preguntones\bingo\Dia.java') `
    (Join-Path $root 'src\com\preguntones\bingo\Premio.java') `
    (Join-Path $root 'src\com\preguntones\bingo\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.bingo.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Bingo pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
$jar = Join-Path $root 'Bingo.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Bingo.jar') -Force
Write-Host 'Bingo.jar listo'
