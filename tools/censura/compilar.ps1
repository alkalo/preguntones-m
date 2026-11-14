param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null

$paper = Join-Path $lib 'paper-api.jar'
if (-not (Test-Path $paper)) {
    $origen = Join-Path $root '..\viajes\lib\paper-api.jar'
    if (Test-Path $origen) {
        Copy-Item $origen $paper
    } else {
        curl.exe -fL -o $paper 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/paper-api-1.21.4-R0.1-SNAPSHOT.jar'
    }
}
$log4jApi = Join-Path $lib 'log4j-api.jar'
$log4jCore = Join-Path $lib 'log4j-core.jar'
$adventure = Join-Path $lib 'adventure-api.jar'
$adventurePlain = Join-Path $lib 'adventure-text-serializer-plain.jar'
$examination = Join-Path $lib 'examination-api.jar'
$jspecify = Join-Path $lib 'jspecify.jar'
if (-not (Test-Path $log4jApi)) {
    curl.exe -fL -o $log4jApi 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.24.3/log4j-api-2.24.3.jar'
}
if (-not (Test-Path $log4jCore)) {
    curl.exe -fL -o $log4jCore 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.24.3/log4j-core-2.24.3.jar'
}
if (-not (Test-Path $adventure)) {
    curl.exe -fL -o $adventure 'https://repo1.maven.org/maven2/net/kyori/adventure-api/4.17.0/adventure-api-4.17.0.jar'
}
if (-not (Test-Path $adventurePlain)) {
    curl.exe -fL -o $adventurePlain 'https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-plain/4.17.0/adventure-text-serializer-plain-4.17.0.jar'
}
if (-not (Test-Path $examination)) {
    curl.exe -fL -o $examination 'https://repo1.maven.org/maven2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar'
}
if (-not (Test-Path $jspecify)) {
    curl.exe -fL -o $jspecify 'https://repo1.maven.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar'
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$cp = "$paper;$log4jApi;$log4jCore;$adventure;$adventurePlain;$examination;$jspecify"
$fuentes = @(
    (Join-Path $root 'src\com\preguntones\censura\Clave.java'),
    (Join-Path $root 'src\com\preguntones\censura\FiltroConsola.java'),
    (Join-Path $root 'src\com\preguntones\censura\CensuraPlugin.java')
)
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @fuentes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
& javac --release 21 -encoding UTF-8 -d $pruebas `
    (Join-Path $root 'src\com\preguntones\censura\Clave.java') `
    (Join-Path $root 'src\com\preguntones\censura\Pruebas.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.censura.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Censura pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
$jar = Join-Path $root 'Censura.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

Copy-Item $jar (Join-Path $root '..\..\servidor\plugins\Censura.jar') -Force
Write-Host 'Censura.jar listo'
