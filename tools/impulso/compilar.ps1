param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$paper = Join-Path $root '..\viajes\lib\paper-api.jar'
$grimLib = Join-Path $root 'lib\grimac-bukkit-2.3.74.jar'
$grimServidor = Join-Path $root '..\..\servidor\plugins\grimac-bukkit-2.3.74.jar'
if (Test-Path $grimLib) {
    $grim = $grimLib
} elseif (Test-Path $grimServidor) {
    $grim = $grimServidor
} else {
    New-Item -ItemType Directory -Force -Path (Split-Path $grimLib) | Out-Null
    & curl.exe -fL -o $grimLib 'https://cdn.modrinth.com/data/LJNGWSvH/versions/baDrVvfo/grimac-bukkit-2.3.74.jar'
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $grimLib)) {
        if (Test-Path $grimLib) { Remove-Item $grimLib -Force }
        throw 'No está Grim'
    }
    $grim = $grimLib
}
if (-not (Test-Path $paper)) { throw 'No está paper-api.jar' }

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$jars = @(Get-ChildItem (Join-Path $root '..\viajes\lib') -Filter *.jar | ForEach-Object { $_.FullName })
$jars += $grim
$cp = $jars -join ';'
$fuentes = @(
    (Join-Path $root 'src\com\preguntones\impulso\Tridente.java'),
    (Join-Path $root 'src\com\preguntones\impulso\ImpulsoPlugin.java')
)
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @fuentes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -proc:none -d $pruebas `
    (Join-Path $root 'src\com\preguntones\impulso\Tridente.java') `
    (Join-Path $root 'src\com\preguntones\impulso\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.impulso.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Impulso pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
$jar = Join-Path $root 'Impulso.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location
Copy-Item $jar (Join-Path $root '..\..\servidor\plugins\Impulso.jar') -Force
Write-Host 'Impulso.jar listo'
