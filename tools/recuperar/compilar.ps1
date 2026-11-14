param(
    [switch]$SoloPruebas
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$censura = Join-Path $root '..\censura\lib'
$paper = Join-Path $censura 'paper-api.jar'
$adventure = Join-Path $root 'lib\adv-4.25.0.jar'
$adventureKey = Join-Path $root 'lib\adventure-key-4.25.jar'
function Buscar-OBajar($nombre, $filtro, $url) {
    $libDir = Join-Path $root 'lib'
    $enLib = Join-Path $libDir $nombre
    if (Test-Path $enLib) { return $enLib }
    foreach ($sitio in @($libDir, (Join-Path $root '..\..\servidor\plugins'))) {
        if (-not (Test-Path $sitio)) { continue }
        $hallado = Get-ChildItem $sitio -Filter $filtro | Select-Object -First 1
        if ($hallado) { return $hallado.FullName }
    }
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    & curl.exe -fL -o $enLib $url
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $enLib)) {
        if (Test-Path $enLib) { Remove-Item $enLib -Force }
        throw "No pude conseguir $nombre"
    }
    return $enLib
}
$authme = Buscar-OBajar 'AuthMe-6.0.1-Paper.jar' 'AuthMe*.jar' 'https://github.com/AuthMe/AuthMeReloaded/releases/download/6.0.1/AuthMe-6.0.1-Paper.jar'
$discord = Buscar-OBajar 'DiscordSRV-Build-1.30.5.jar' 'DiscordSRV*.jar' 'https://github.com/DiscordSRV/DiscordSRV/releases/download/v1.30.5/DiscordSRV-Build-1.30.5.jar'
$adventurePlain = Join-Path $censura 'adventure-text-serializer-plain.jar'
$examination = Join-Path $censura 'examination-api.jar'
$jspecify = Join-Path $censura 'jspecify.jar'
$log4j = Join-Path $censura 'log4j-api.jar'
$chat = Join-Path $root '..\viajes\lib\bungeecord-chat-1.20-R0.2.jar'
if (-not (Test-Path $paper)) { throw "Falta paper-api.jar" }
if (-not (Test-Path $adventure)) { throw "Falta lib\adv-4.25.0.jar" }
if (-not (Test-Path $adventureKey)) { throw "Falta lib\adventure-key-4.25.jar" }
if (-not (Test-Path $authme)) { throw "Falta AuthMe-6.0.1-Paper.jar" }
if (-not $discord) { throw "Falta DiscordSRV.jar" }

$stubs = Join-Path $root 'build-stubs'
if (Test-Path $stubs) { Remove-Item $stubs -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stubs | Out-Null
$fuentesStub = Get-ChildItem (Join-Path $root 'stubs') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& javac --release 21 -encoding UTF-8 -proc:none -cp "$adventure;$adventureKey;$paper;$examination;$jspecify" -d $stubs @fuentesStub
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path $build | Out-Null
$cp = "$adventure;$adventureKey;$stubs;$paper;$adventurePlain;$examination;$jspecify;$log4j;$chat;$authme;$discord"
$fuentes = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java' |
    Where-Object { $_.Name -ne 'Pruebas.java' } |
    ForEach-Object { $_.FullName }
& javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $build @fuentes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$pruebas = Join-Path $root 'build-pruebas'
if (Test-Path $pruebas) { Remove-Item $pruebas -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pruebas | Out-Null
$puras = @(
    'Codigo.java', 'Huella.java', 'Pedido.java', 'Politica.java', 'Aviso.java', 'Pruebas.java'
) | ForEach-Object { Join-Path $root "src\com\preguntones\recuperar\$_" }
& javac --release 21 -encoding UTF-8 -d $pruebas @puras
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $pruebas com.preguntones.recuperar.Pruebas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($SoloPruebas) {
    Write-Host 'Recuperar pruebas ok'
    exit 0
}

Copy-Item (Join-Path $root 'plugin.yml') $build
$jar = Join-Path $root 'Recuperar.jar'
if (Test-Path $jar) { Remove-Item $jar }
Push-Location $build
& jar cf $jar .
Pop-Location
Copy-Item $jar (Join-Path $root '..\..\servidor\plugins\Recuperar.jar') -Force
Write-Host 'Recuperar.jar listo'
