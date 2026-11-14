param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$vault = Join-Path $root 'lib\vault-api.jar'
if (-not (Test-Path $vault) -or (Get-Item $vault).Length -lt 1000) {
    $copias = @(
        (Join-Path $root '..\lib-compile\Vault.jar'),
        (Join-Path $root '..\bingo\lib\vault-api.jar'),
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

$api = Join-Path $root 'lib\paper-api.jar'
if (-not (Test-Path $api)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $api) | Out-Null
    $url = 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-20260511.115010-91.jar'
    & curl.exe -fL -o $api $url
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $api)) {
        if (Test-Path $api) { Remove-Item $api -Force }
        throw 'No pude bajar paper-api'
    }
}

$companeras = @{
    'adventure-api-4.26.1.jar' = 'https://repo1.maven.org/maven2/net/kyori/adventure-api/4.26.1/adventure-api-4.26.1.jar'
    'adventure-key-4.26.1.jar' = 'https://repo1.maven.org/maven2/net/kyori/adventure-key/4.26.1/adventure-key-4.26.1.jar'
    'adventure-nbt-4.26.1.jar' = 'https://repo1.maven.org/maven2/net/kyori/adventure-nbt/4.26.1/adventure-nbt-4.26.1.jar'
    'annotations-26.0.2-1.jar' = 'https://repo1.maven.org/maven2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar'
    'bungeecord-chat-1.20-R0.2.jar' = 'https://repo1.maven.org/maven2/net/md-5/bungeecord-chat/1.20-R0.2/bungeecord-chat-1.20-R0.2.jar'
    'examination-api-1.3.0.jar' = 'https://repo1.maven.org/maven2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar'
    'examination-string-1.3.0.jar' = 'https://repo1.maven.org/maven2/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar'
    'joml-1.10.8.jar' = 'https://repo1.maven.org/maven2/org/joml/joml/1.10.8/joml-1.10.8.jar'
}
$libDir = Join-Path $root 'lib'
New-Item -ItemType Directory -Force -Path $libDir | Out-Null
foreach ($nombre in $companeras.Keys) {
    $destinoLib = Join-Path $libDir $nombre
    if (Test-Path $destinoLib) { continue }
    & curl.exe -fL -o $destinoLib $companeras[$nombre]
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $destinoLib)) {
        if (Test-Path $destinoLib) { Remove-Item $destinoLib -Force }
        throw "No pude bajar $nombre"
    }
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$jars = Get-ChildItem (Join-Path $root 'lib') -Filter *.jar | ForEach-Object { $_.FullName }
$cp = $jars -join ';'
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | Where-Object { $_.Name -ne 'Pruebas.java' }
& javac --release 21 -encoding UTF-8 -cp $cp -d $build @($fuentes.FullName)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
$puras = @(
    (Join-Path $root 'src\com\preguntones\viajes\Casa.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Huella.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Consentir.java'),
    (Join-Path $root 'src\com\preguntones\viajes\NombreBandera.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Peligro.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Radio.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Reloj.java'),
    (Join-Path $root 'src\com\preguntones\viajes\Pruebas.java')
)
& javac --release 21 -encoding UTF-8 -d $pruebas @puras
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.viajes.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Viajes pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
Copy-Item (Join-Path $root 'config.yml') $build
Copy-Item (Join-Path $root 'panel.json') $build
$panelMundo = Join-Path $root '..\..\servidor\world\datapacks\preguntones\data\preguntones\dialog\panel.json'
if (Test-Path (Split-Path $panelMundo)) {
    Copy-Item (Join-Path $root 'panel.json') $panelMundo -Force
}
$jar = Join-Path $root 'Viajes.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location

$destino = Join-Path $root '..\..\servidor\plugins\Viajes.jar'
Copy-Item $jar $destino -Force
Write-Host "Viajes.jar listo"
