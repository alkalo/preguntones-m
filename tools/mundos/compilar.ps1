param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$vault = Join-Path $lib 'vault-api.jar'
if (-not (Test-Path $vault)) {
    $origen = Join-Path $root '..\lib-compile\Vault.jar'
    if (Test-Path $origen) {
        Copy-Item $origen $vault
    } else {
        curl.exe -fL -o $vault 'https://repo1.maven.org/maven2/com/github/MilkBowl/VaultAPI/1.7.1/VaultAPI-1.7.1.jar'
    }
}
$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$jars = @(Get-ChildItem (Join-Path $root '..\viajes\lib') -Filter *.jar | ForEach-Object { $_.FullName })
$jars += $vault
$cp = $jars -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -d $pruebas (Join-Path $root 'src\com\preguntones\mundos\Ciclo.java') (Join-Path $root 'src\com\preguntones\mundos\Pelea.java') (Join-Path $root 'src\com\preguntones\mundos\Progresos.java') (Join-Path $root 'src\com\preguntones\mundos\Esquina.java') (Join-Path $root 'src\com\preguntones\mundos\Fuego.java') (Join-Path $root 'src\com\preguntones\mundos\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.mundos.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Mundos pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
Copy-Item (Join-Path $root 'progresos-pack') (Join-Path $build 'progresos-pack') -Recurse
$jar = Join-Path $root 'Mundos.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$chunky = Join-Path $lib 'Chunky-Paper-1.5.66.jar'
if (-not (Test-Path $chunky)) {
    & curl.exe -fL -o $chunky 'https://cdn.modrinth.com/data/fALzjamp/versions/RBuKjN4k/Chunky-Paper-1.5.66.jar'
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $chunky)) { throw 'No pude bajar Chunky' }
}
$destino = Join-Path $root '..\..\servidor\plugins'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
Copy-Item $jar (Join-Path $destino 'Mundos.jar') -Force
Copy-Item $chunky (Join-Path $destino 'Chunky-Paper-1.5.66.jar') -Force
Write-Host 'Mundos.jar listo'
